package com.pydio.android.cells.ui.login.models

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.legacy.P8Credentials
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.MalformedURLException
import javax.net.ssl.SSLException

class LoginVM(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = "LoginVM"

    // UI
    // Add some delay for the end user to be aware of what is happening under the hood.
    // TODO remove or reduce
    private val smoothActionDelay = 750L

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String?> = _message.asStateFlow()

    private var _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Business methods
    fun flush() {
        viewModelScope.launch {
            resetMessages()
        }
    }

    suspend fun pingAddress(url: String, skipVerify: Boolean): String? {
        val res = processAddress(url, skipVerify)
        if (Str.notEmpty(res)) {
            _message.value = ""
        }
        return res
    }

    suspend fun confirmSkipVerifyAndPing(url: String): String? {
        return processAddress(url, true)
    }

    suspend fun logToP8(
        url: String,
        skipVerify: Boolean,
        login: String,
        password: String,
        captcha: String?
    ): StateID? {
        switchLoading(true)
        return doP8Auth(url, skipVerify, login, password, captcha)
    }

    suspend fun getSessionView(accountID: StateID): RSessionView? = withContext(Dispatchers.IO) {
        accountService.getSession(accountID)
    }

    suspend fun handleOAuthResponse(state: String, code: String): Pair<StateID, String?>? {
        Log.e(logTag, "Handling OAuth response")

        switchLoading(true)
        updateMessage("Retrieving authentication token...")
        val res = withContext(Dispatchers.IO) {
            delay(smoothActionDelay)
            authService.handleOAuthResponse(accountService, sessionFactory, state, code)
        } ?: run {
            updateErrorMsg("could not retrieve token from code")
            return null
        }

        updateMessage("Updating account info...")
        // Log.d(logTag, "Configuring account ${StateID.fromId(res.first)} before ${res.second}")
        // FIXME handle "next" page
        res.second?.let {
            Log.e(logTag, "Unhandled next action: $it")
        }
        val res2 = withContext(Dispatchers.IO) {
            val tmpResult = accountService.refreshWorkspaceList(res.first)
            delay(smoothActionDelay)
            tmpResult
        }

        return res2.second?.let {
            updateErrorMsg(it)
            null
        } ?: run {
            res
        }
    }

    // Internal helpers

    /** Returns the route for the next destination if we have to move to next page */
    private suspend fun processAddress(url: String, skipVerify: Boolean): String? {

        if (Str.empty(url)) {
            updateErrorMsg("Server address is empty, could not proceed")
            return null
        }
        switchLoading(true)

        // 1) Ping the server and check if:
        //   - address is valid
        //   - distant server has a valid TLS configuration
        val pingResult = doPing(url, skipVerify)
        val serverURL = pingResult.first ?: run {
            if (skipVerify) { // ping fails also with skip verify.
                // TODO handle the case
            }
            // We assume we always have the skip verify route here
            return pingResult.second
        }

        Log.e(logTag, "after ping, server URL: $serverURL")

        // ServerURL is OK aka 200 at given URL with correct cert
        updateMessage("Address and cert are valid. Registering server...")

        //  2) Register the server locally
        val server = doRegister(serverURL)
            ?: // Error messages and states are handled above
            return null

        val serverID = StateID(server.url())
        // 3) Specific login process depending on the remote server type (Cells or P8).
        switchLoading(false)
        return if (server.isLegacy) {
            LoginDestinations.P8Credentials.createRoute(serverID, skipVerify)
        } else {
//            viewModelScope.launch {
//                triggerOAuthProcess(serverURL)
//            }
            // FIXME this is not satisfying: error won't be processed correctly
            return LoginDestinations.LaunchAuthProcessing.createRoute(serverID, skipVerify)
        }
    }

    /**
     * Returns a ServerURL if the ping is successful or a route to navigate
     * to the skip verify step if we got a SSL exception
     */
    private suspend fun doPing(
        serverAddress: String,
        skipVerify: Boolean
    ): Pair<ServerURL?, String?> {
        return withContext(Dispatchers.IO) {
            Log.i(logTag, "Pinging $serverAddress")
            val tmpURL: ServerURL?
            var newURL: ServerURL? = null
            try {
                tmpURL = ServerURLImpl.fromAddress(serverAddress, skipVerify)
                tmpURL.ping()
                newURL = tmpURL
            } catch (e: MalformedURLException) {
                Log.e(logTag, "Invalid address: [$serverAddress]. Cause:  ${e.message} ")
                updateErrorMsg(e.message ?: "Invalid address, please update")
            } catch (e: SSLException) {
                updateErrorMsg("Invalid certificate for $serverAddress")
                Log.e(logTag, "Invalid certificate for $serverAddress: ${e.message}")
                // We might do this better with exceptions once with have a valid strategy for error handling with coroutines
                return@withContext null to LoginDestinations.SkipVerify.createRoute(
                    StateID(
                        serverAddress
                    )
                )
            } catch (e: IOException) {
                updateErrorMsg("IOException: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                updateErrorMsg(e.message ?: "Invalid address, please update")
                e.printStackTrace()
            }
            Pair(newURL, null)
        }
    }

    private suspend fun doRegister(su: ServerURL): Server? {
        return try {
            val newServer = withContext(Dispatchers.IO) {
                sessionFactory.registerServer(su)
            }
            newServer
        } catch (e: SDKException) {
            val msg = "${su.url.host} does not seem to be a Pydio server"
            updateErrorMsg("$msg. Please, double-check.")
            Log.e(logTag, "$msg - Err.${e.code}: ${e.message}")
            // TODO double check
            // navigateBack()
            // TODO setCurrentStep(LoginStep.URL)
            null
        }
    }

    private suspend fun doP8Auth(
        url: String,
        skipVerify: Boolean,
        login: String,
        password: String,
        captcha: String?
    ): StateID? {
        val currURL = ServerURLImpl.fromAddress(url, skipVerify)
        val credentials = P8Credentials(login, password, captcha)
        val msg = "Launch P8 auth process for ${credentials.username}@${currURL.url}"
        Log.i(logTag, msg)
        updateMessage(msg)

        val accountID = withContext(Dispatchers.IO) {
            var tmpID: StateID? = null
            try {
                tmpID = accountService.signUp(currURL, credentials)
                delay(smoothActionDelay)
                updateMessage("Connected, updating local state")
//                withContext(Dispatchers.Main) {
//                    setCurrentStep(LoginStep.PROCESS_AUTH)
//                }
                accountService.refreshWorkspaceList(tmpID)
                delay(smoothActionDelay)
            } catch (e: SDKException) {
                // TODO handle captcha here
                Log.e(logTag, "${e.code}: ${e.message}")
                e.printStackTrace()
                updateErrorMsg(e.message ?: "Invalid credentials, please try again")
            }
            tmpID
        }

        return accountID
    }

    suspend fun newOAuthIntent(serverURL: ServerURL, nextAction: String): Intent? =
        withContext(Dispatchers.Main) {
            updateMessage("Launching OAuth credential flow")
            val uri = try {
                authService.generateOAuthFlowUri(
                    sessionFactory,
                    serverURL,
                    nextAction,
                )

            } catch (e: SDKException) {
                val msg =
                    "Cannot get uri for ${serverURL.url.host}, cause: ${e.code} - ${e.message}"
                Log.e(logTag, msg)
                updateErrorMsg(msg)
                return@withContext null
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            Log.e(logTag, "Intent created: ${intent.data}")
            return@withContext intent
        }

    // UI Methods
    private fun switchLoading(newState: Boolean) {
        if (newState) { // also remove old error message when we start a new processing
            _errorMessage.value = ""
        }
        _isProcessing.value = newState
    }

    private suspend fun updateMessage(msg: String) {
        withContext(Dispatchers.Main) {
            _message.value = msg
            // or yes ?? TODO switchLoading(true)
        }
    }

    private suspend fun updateErrorMsg(msg: String) {
        withContext(Dispatchers.Main) {
            _errorMessage.value = msg
            _message.value = ""
            switchLoading(false)
        }
    }

    suspend fun resetMessages() {
        withContext(Dispatchers.Main) {
            _errorMessage.value = ""
            _message.value = ""
            switchLoading(false)
        }
    }

    override fun onCleared() {
        Log.e(logTag, "################## onCleared")
        super.onCleared()
    }

    init {
        Log.e(logTag, "################## Created")
    }
}
