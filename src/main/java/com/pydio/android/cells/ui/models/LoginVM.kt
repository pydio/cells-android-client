package com.pydio.android.cells.ui.models

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.legacy.P8Credentials
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.MalformedURLException
import javax.net.ssl.SSLException

enum class LoginStep {
    URL, SKIP_VERIFY, P8_CRED, OAUTH_FLOW, POST_AUTH, DONE, ERROR
}

enum class AfterLogin {
    ACCOUNTS, BROWSE, TERMINATE, SELECT_TARGET
}

/**
 * Main view model for the login process with both Cells and P8.
 * We also rely on the AuthActivity to gather info when called from outside this activity.
 */
class LoginVM(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = "LoginVM"
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // UI
    // Tracks current page
    private val _currDestination = MutableStateFlow(LoginStep.URL)
    val currDestination: StateFlow<LoginStep>
        get() = _currDestination

    // True if a request is currently running
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean>
        get() = _isProcessing

    private val _message = MutableStateFlow("")
    val message: StateFlow<String?>
        get() = _message

    private var _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String?>
        get() = _errorMessage

    private var _oauthIntent: MutableStateFlow<Intent?> = MutableStateFlow(null)
    val oauthIntent: StateFlow<Intent?>
        get() = _oauthIntent

    // Business
    // First step, we have nothing then an address
    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String>
        get() = _serverAddress

    // Handle TLS if necessary
    private val _skipVerify = MutableStateFlow(false)
//    private val _invalidTLS = MutableStateFlow(false)
//    val invalidTLS: StateFlow<Boolean>
//        get() = _invalidTLS

    // A valid server URL with TLS managed
    private val _serverUrl: MutableStateFlow<ServerURL?> = MutableStateFlow(null)
    val serverUrl: StateFlow<ServerURL?>
        get() = _serverUrl

    // Server is a Pydio instance and has already been registered in local repository
    private var _server: MutableStateFlow<Server?> = MutableStateFlow(null)
    val server: StateFlow<Server?>
        get() = _server

    // Set upon successful authentication against the remote server
    private val _accountID: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountID: StateFlow<String?>
        get() = _accountID

    private var _nextAction = AuthService.NEXT_ACTION_BROWSE
    val nextAction: String
        get() = _nextAction

    // UI Methods
    fun after(currStep: LoginStep) {
        val nextStep = when (currStep) {
            LoginStep.URL -> LoginStep.P8_CRED
            LoginStep.P8_CRED -> LoginStep.POST_AUTH
            else -> LoginStep.URL
        }
        Log.e(logTag, "In after: $nextStep, and ${_currDestination.value}")
        _currDestination.value = nextStep
    }

    fun setCurrentStep(currStep: LoginStep) {
        // Dirty tweak otherwise we have an issue when using the back button of the system
        _currDestination.value = currStep
        switchLoading(false)
        _message.value = ""
    }

    fun toP8Credentials(urlStr: String, next: String) {
        // TODO rather retrieve the Server from the local repo
        _nextAction = next
        _serverUrl.value = ServerURLImpl.fromJson(urlStr)
        _currDestination.value = LoginStep.P8_CRED
    }

    suspend fun toCellsCredentials(urlStr: String, next: String) {
        // TODO rather retrieve the Server from the local repo
        _nextAction = next
        val url = ServerURLImpl.fromJson(urlStr)
        _serverUrl.value = url
        _currDestination.value = LoginStep.OAUTH_FLOW
        triggerOAuthProcess(url)
    }

    // Business methods
    suspend fun pingAddress(serverAddress: String) {
        _serverAddress.value = serverAddress
        processAddress()
    }

    suspend fun confirmSkipVerifyAndPing() {
        _skipVerify.value = true
        processAddress()
    }

    fun logToP8(login: String, password: String, captcha: String?) {
        // TODO validate passed parameters
        switchLoading(true)
        vmScope.launch {
            val ok = doP8Auth(login, password, captcha)
            if (ok) {
                setCurrentStep(LoginStep.DONE)
            }
        }
    }

    suspend fun handleOAuthResponse(state: String, code: String) {
        setCurrentStep(LoginStep.POST_AUTH)
        updateMessage("Retrieving authentication token...")
        val newAccount = withContext(Dispatchers.IO) {
            authService.handleOAuthResponse(accountService, sessionFactory, state, code)
        } ?: run {
            updateErrorMsg("could not retrieve token from code")
            return
        }

        updateMessage("Configuring account...")
        newAccount.second?.let { _nextAction = it }
        withContext(Dispatchers.IO) {
            accountService.refreshWorkspaceList(newAccount.first)
            delay(2000)
        }

        _accountID.value = newAccount.first
        setCurrentStep(LoginStep.DONE)
    }

    // Internal stuff
    suspend fun processAddress() {
        if (Str.empty(_serverAddress.value)) {
            updateErrorMsg("Server address is empty, could not proceed")
        }
        switchLoading(true)
        // First Ping the server and check if address is valid
        // and distant server has a valid TLS configuration
        val serverURL = doPing(_serverAddress.value)
            ?: // Error Message is handled by the doPing
            return

        Log.e(logTag, "after ping, server URL: $serverURL")

        // ServerURL is OK aka 200 at given URL with correct cert
        _serverUrl.value = serverURL
        updateMessage("Address and cert are valid. Registering server...")

        //  tries to register server
        val server = doRegister(serverURL)
            ?: // Error messages and states are handled above
            return
        updateMessage("")
        _server.value = server
        // make progress + 1

        if (server.isLegacy) {
            _currDestination.value = LoginStep.P8_CRED
        } else {
            triggerOAuthProcess(serverURL)
        }
    }


    private suspend fun doPing(serverAddress: String): ServerURL? {
        return withContext(Dispatchers.IO) {
            Log.i(logTag, "Perform real ping to $serverAddress")
            var newURL: ServerURL? = null
            try {
                newURL = ServerURLImpl.fromAddress(serverAddress, _skipVerify.value)
                newURL.ping()
            } catch (e: MalformedURLException) {
                Log.e(logTag, e.message ?: "Invalid address, please update")
                updateErrorMsg(e.message ?: "Invalid address, please update")
            } catch (e: SSLException) {
                updateErrorMsg("Invalid certificate for $serverAddress")
                Log.e(logTag, "Invalid certificate for $serverAddress: ${e.message}")

                withContext(Dispatchers.Main) {
                    _skipVerify.value = false
                    // _invalidTLS.value = true
                    _currDestination.value = LoginStep.SKIP_VERIFY
                }
                return@withContext null
            } catch (e: IOException) {
                updateErrorMsg(e.message ?: "IOException:")
                e.printStackTrace()
            } catch (e: Exception) {
                updateErrorMsg(e.message ?: "Invalid address, please update")
                e.printStackTrace()
            }
            newURL
        }
    }

    private suspend fun doRegister(su: ServerURL): Server? = withContext(Dispatchers.IO) {
        Log.d(logTag, "About to register the server ${su.id}")
        var newServer: Server? = null
        try {
            newServer = sessionFactory.registerServer(su)
        } catch (e: SDKException) {
            updateErrorMsg(
                e.message
                    ?: "This does not seem to be a Pydio server address, please double check"
            )
        }
        newServer
    }

    private suspend fun doP8Auth(login: String, password: String, captcha: String?): Boolean {
        val currURL = serverUrl.value
            ?: run {
                updateErrorMsg("No server URL defined, cannot start P8 auth process")
                return false
            }

        val credentials = P8Credentials(login, password, captcha)
        val msg = "Launch P8 auth process for ${credentials.username}@${currURL.url}"
        Log.i(logTag, msg)
        updateMessage(msg)

        val accountIDStr = withContext(Dispatchers.IO) {
            var id: String? = null
            try {
                id = accountService.signUp(currURL, credentials)
                updateMessage("Connected, refreshing local state")

                withContext(Dispatchers.Main) {
                    setCurrentStep(LoginStep.POST_AUTH)
                }

                accountService.refreshWorkspaceList(id)
                delay(2000)
            } catch (e: SDKException) {
                // TODO handle captcha here
                Log.e(logTag, "${e.code}: ${e.message}")
                updateErrorMsg(e.message ?: "Invalid credentials, please try again")
            }
            id
        }

        if (accountIDStr != null) {
            _accountID.value = accountIDStr
            return true
        }
        return false
    }

    private suspend fun triggerOAuthProcess(serverURL: ServerURL) {
        updateMessage("Launching OAuth credential flow")
        vmScope.launch {
            val intent = authService.createOAuthIntent(
                sessionFactory,
                serverURL,
                AuthService.NEXT_ACTION_BROWSE
            )
            Log.e(logTag, "Got an intent for ${intent?.data}")
            _oauthIntent.value = intent
            // switchLoading(false)
        }
    }

    // Helpers
    private fun switchLoading(newState: Boolean) {
        _isProcessing.value = newState
    }

    private suspend fun updateErrorMsg(msg: String) {
        withContext(Dispatchers.Main) {
            _errorMessage.value = msg
            switchLoading(false)

        }
    }

    private suspend fun updateMessage(msg: String) {
        withContext(Dispatchers.Main) {
            _message.value = msg
            // or yes ?? TODO switchLoading(true)
        }
    }

    override fun onCleared() {
        Log.d(logTag, "onCleared")
        super.onCleared()
        viewModelJob.cancel()
    }

    init {
        Log.d(logTag, "Created")
    }

}
