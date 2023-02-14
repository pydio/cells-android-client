package com.pydio.android.cells.ui.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
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

class LoginViewModelNew(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = LoginViewModelNew::class.simpleName

    // UI
    // Add some delay for the end user to be aware of what is happening under the hood.
    // TODO remove or reduce
    private val smoothActionDelay = 750L

    // Tracks current page
//    private val _currDestination = MutableStateFlow(LoginStep.URL)
//    val currDestination: StateFlow<LoginStep> = _currDestination.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String?> = _message.asStateFlow()

    private var _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Used to trigger an "external call"
    private var _oauthIntent: MutableStateFlow<Intent?> = MutableStateFlow(null)
    val oauthIntent: StateFlow<Intent?> = _oauthIntent.asStateFlow()

    // Business Data: TODO we don't need flows for these variables
    // First step, we have nothing then an address
    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress

    // Handle TLS if necessary
    private val _skipVerify = MutableStateFlow(false)

    // A valid server URL with TLS managed
    private val _serverUrl: MutableStateFlow<ServerURL?> = MutableStateFlow(null)
    val serverUrl: StateFlow<ServerURL?> = _serverUrl

    // Used when relogging a P8 Server
    private val _username: MutableStateFlow<String?> = MutableStateFlow(null)
    val username: StateFlow<String?> = _username

    // Server is a Pydio instance and has already been registered in local repository
    private var _server: MutableStateFlow<Server?> = MutableStateFlow(null)
    val server: StateFlow<Server?> = _server

    // Set upon successful authentication against the remote server
    private val _accountID: MutableStateFlow<String?> = MutableStateFlow(null)
    val accountID: StateFlow<String?> = _accountID

    private var _nextAction: MutableStateFlow<String> =
        MutableStateFlow(AuthService.NEXT_ACTION_BROWSE)
    val nextAction: StateFlow<String> = _nextAction

    // UI Methods
    fun switchLoading(newState: Boolean) {
        if (newState) { // also remove old error message when we start a new processing
            _errorMessage.value = ""
        }
        _isProcessing.value = newState
    }

//    fun after2(currStep: LoginStep) {
//
//        when (currStep) {
//            LoginStep.URL -> navigateTo(P8CredsRoute.route)
//            LoginStep.P8_CRED -> navigateTo(P8CredsRoute.route) // FIXME LoginStep.PROCESS_AUTH
//            // FIXME else -> LoginStep.URL
//            else -> return // Do nothing for the time being
//        }
//    }


//    fun after(currStep: LoginStep) {
//        val nextStep = when (currStep) {
//            LoginStep.URL -> LoginStep.P8_CRED
//            LoginStep.P8_CRED -> LoginStep.PROCESS_AUTH
//            else -> LoginStep.URL
//        }
//        Log.e(logTag, "After: $currStep, going to ${nextStep}")
//
//
//
//        _currDestination.value = nextStep
//    }

//    fun setCurrentStep(currStep: LoginStep) {
//        // Dirty tweak otherwise we have an issue when using the back button of the system
//        if (currStep != _currDestination.value) {
//            _currDestination.value = currStep
//            switchLoading(false)
//            _message.value = ""
//        } else {
//            // Tmp log to monitor this behaviour
//            Log.i(logTag, "Skipping state update for same step: $currStep")
//        }
//    }


    // Business methods
    fun setAddress(serverAddress: String) {
        _serverAddress.value = serverAddress.trim()
        _errorMessage.value = ""
    }

    suspend fun pingAddress(): String? {
        val res = processAddress()
        if (Str.notEmpty(res)) {
            _message.value = ""
        }
        return res
    }

    suspend fun confirmSkipVerifyAndPing(): String? {
        _skipVerify.value = true
        return pingAddress()
    }

    suspend fun logToP8(login: String, password: String, captcha: String?): Boolean {
        // TODO validate passed parameters
        switchLoading(true)
        return doP8Auth(login, password, captcha)
    }


    suspend fun getSessionView(accountID: StateID): RSessionView? = withContext(Dispatchers.IO) {
        accountService.getSession(accountID)
    }

    // FIXME to re-add direct login to P8 feature

//    fun toP8Credentials(urlStr: String, next: String) : String {
//        // TODO rather retrieve the Server from the local repo
//        val url = ServerURLImpl.fromJson(urlStr)
//        _nextAction.value = next
//        _serverUrl.value = url
//        if (Str.empty(_serverAddress.value)) { // tweak to have an URL if the user clicks back
//            _serverAddress.value = url.id
//            if (url.skipVerify()) {
//                _skipVerify.value = true
//            }
//        }
//        navigateTo(P8CredsRoute.route)
//        // _currDestination.value = LoginStep.P8_CRED
//    }

    suspend fun toCellsCredentials(urlStr: String, next: String) {
        // TODO rather retrieve the Server from the local repo
        _nextAction.value = next
        val url = ServerURLImpl.fromJson(urlStr)
        _serverUrl.value = url
        if (Str.empty(_serverAddress.value)) { // tweak to have an URL if the user clicks back
            _serverAddress.value = url.id
            if (url.skipVerify()) {
                _skipVerify.value = true
            }
        }
        triggerOAuthProcess(url)
    }

    suspend fun toCellsCredentials(sessionView: RSessionView, next: String) {
        _nextAction.value = next
        val url = ServerURLImpl.fromAddress(sessionView.url, sessionView.skipVerify())
        _serverUrl.value = url
        if (sessionView.isLegacy){
            _username.value = sessionView.username
        } else {
            triggerOAuthProcess(url)
        }
    }

    suspend fun handleOAuthResponse(state: String, code: String): Boolean {

        switchLoading(true)
        updateMessage("Retrieving authentication token...")
        val res = withContext(Dispatchers.IO) {
            delay(smoothActionDelay)
            authService.handleOAuthResponse(accountService, sessionFactory, state, code)
        } ?: run {
            updateErrorMsg("could not retrieve token from code")
            return false
        }

        updateMessage("Updating account info...")
        // Log.d(logTag, "Configuring account ${StateID.fromId(res.first)} before ${res.second}")
        res.second?.let { _nextAction.value = it }
        val res2 = withContext(Dispatchers.IO) {
            val res = accountService.refreshWorkspaceList(res.first)
            delay(smoothActionDelay)
            res
        }

        _accountID.value = res.first

        return Str.empty(res2.second)
    }

    // Internal helpers

    private suspend fun processAddress(): String? {
        if (Str.empty(_serverAddress.value)) {
            updateErrorMsg("Server address is empty, could not proceed")
            return null
        }
        switchLoading(true)
        // 1) Ping the server and check if:
        //   - address is valid
        //   - distant server has a valid TLS configuration
        val serverURL = doPing(_serverAddress.value)
            ?: // Error Message is handled by the doPing
            return null

        Log.e(logTag, "after ping, server URL: $serverURL")

        // ServerURL is OK aka 200 at given URL with correct cert
        _serverUrl.value = serverURL
        updateMessage("Address and cert are valid. Registering server...")

        //  2) Register the server locally
        val server = doRegister(serverURL)
            ?: // Error messages and states are handled above
            return null
        _server.value = server

        // 3) Specific login process depending on the remote server type (Cells or P8).
        if (server.isLegacy) {
            return RouteLoginP8Credentials.route
        } else {
            viewModelScope.launch {
                triggerOAuthProcess(serverURL)
            }
            // FIXME this is not satisfying: error won't be processed correctly
            return RouteLoginProcessAuth.route
        }
    }

    private suspend fun doPing(serverAddress: String): ServerURL? {
        return withContext(Dispatchers.IO) {
            Log.i(logTag, "Pinging $serverAddress")
            val tmpURL: ServerURL?
            var newURL: ServerURL? = null
            try {
                tmpURL = ServerURLImpl.fromAddress(serverAddress, _skipVerify.value)
                tmpURL.ping()
                newURL = tmpURL
            } catch (e: MalformedURLException) {
                Log.e(logTag, "Invalid address: [$serverAddress]. Cause:  ${e.message} ")
                updateErrorMsg(e.message ?: "Invalid address, please update")
            } catch (e: SSLException) {
                updateErrorMsg("Invalid certificate for $serverAddress")
                Log.e(logTag, "Invalid certificate for $serverAddress: ${e.message}")
                withContext(Dispatchers.Main) {
                    _skipVerify.value = false
                    // TODO _currDestination.value = LoginStep.SKIP_VERIFY
                    // FIXME
                    //  navigateTo(SkipVerifyRoute.route)
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
                delay(smoothActionDelay)
                updateMessage("Connected, updating local state")
//                withContext(Dispatchers.Main) {
//                    setCurrentStep(LoginStep.PROCESS_AUTH)
//                }
                accountService.refreshWorkspaceList(id)
                delay(smoothActionDelay)
            } catch (e: SDKException) {
                // TODO handle captcha here
                Log.e(logTag, "${e.code}: ${e.message}")
                e.printStackTrace()
                updateErrorMsg(e.message ?: "Invalid credentials, please try again")
            }
            id
        }

        return if (accountIDStr != null) {
            _accountID.value = accountIDStr
            true
            // FIXME
            // setCurrentStep(LoginStep.DONE)
        } else
            return false
    }

    private suspend fun triggerOAuthProcess(serverURL: ServerURL) {
        updateMessage("Launching OAuth credential flow")
        withContext(Dispatchers.Main) {
            val uri = try {
                authService.generateOAuthFlowUri(
                    sessionFactory,
                    serverURL,
                    _nextAction.value,
                )

            } catch (e: SDKException) {
                val msg =
                    "Cannot get uri for ${serverURL.url.host}, cause: ${e.code} - ${e.message}"
                Log.e(logTag, msg)
                updateErrorMsg(msg)
                return@withContext
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY

            Log.d(logTag, "Intent created: ${intent?.data}")
            _oauthIntent.value = intent
        }
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

    override fun onCleared() {
        Log.e(logTag, "################## onCleared")
        super.onCleared()
    }

    init {
        Log.e(logTag, "################## Created")
    }
}
