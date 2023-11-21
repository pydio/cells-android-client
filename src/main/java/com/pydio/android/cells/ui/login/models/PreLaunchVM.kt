package com.pydio.android.cells.ui.login.models

import android.content.ClipData
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.ui.AppState
import com.pydio.android.cells.ui.share.ShareDestinations
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PreLaunchState {
    NEW, PROCESSING, DONE, SKIP
}

enum class KnownIntent {
    LAUNCH, RE_LOG, SHARE, SHARE_MULTI
}

class PreLaunchVM(
    private val prefs: PreferencesService,
    coroutineService: CoroutineService,
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
    private val intentID: String
) : ViewModel() {

    private val logTag = "OAuthVM"
    private val smoothActionDelay = 2000L
    private val ioDispatcher = coroutineService.ioDispatcher
    private val uiDispatcher = coroutineService.uiDispatcher

    private var _currIntent: KnownIntent = KnownIntent.LAUNCH
    val currIntent = _currIntent

    private val _processState = MutableStateFlow(PreLaunchState.NEW)
    val processState: StateFlow<PreLaunchState> = _processState.asStateFlow()

    private val _appState = MutableStateFlow(AppState.NONE)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // Login process
    private val _accountID = MutableStateFlow<StateID?>(null)
    private val _loginContext = MutableStateFlow<String?>(null)
    val accountID: StateFlow<StateID?> = _accountID.asStateFlow()
    val loginContext: StateFlow<String?> = _loginContext.asStateFlow()

    // Share process
    private val _uris: MutableList<Uri> = mutableListOf()
    private val uris: List<Uri> = _uris
    private var _targetID: StateID = StateID.NONE
    private val targetID: StateID = _targetID

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String?> = _message.asStateFlow()

    private var _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

//    suspend fun getStartingState(): StartingState {
//        val stateID: StateID?
//        // TODO get latest known state from preferences and navigate to it
//
//        // Fallback on defined accounts:
//        val sessions = accountService.listSessionViews(true)
//        stateID = when (sessions.size) {
//            0 -> null
//            1 -> sessions[0].getStateID()
//            else -> {
//                // If a session is listed as in foreground, we open this one
//                accountService.getActiveSession()?.getStateID() ?: StateID.NONE
//            }
//        }
//
//        val state = StartingState(stateID ?: StateID.NONE)
//        state.route = when (stateID) {
//            null -> LoginDestinations.AskUrl.createRoute()
//            StateID.NONE -> CellsDestinations.Accounts.route
//            else -> BrowseDestinations.Open.createRoute(stateID)
//        }
//        return state
//    }


    fun skip() {
        _processState.value = PreLaunchState.SKIP
    }

    fun launchApp() {
        _currIntent = KnownIntent.LAUNCH
        _appState.value = AppState(
            stateID = StateID.NONE,
            intentID = intentID,
            // TODO maybe also insure here that we have a first correct destination, see above.
            route = null,
            context = null
        )
        _processState.value = PreLaunchState.DONE
    }

    /** Simply checks if the returned state is bound to a known in-progress OAuth flow.*/
    suspend fun isAuthStateValid(state: String): Pair<Boolean, StateID> {
        return authService.isAuthStateValid(state)
    }

    /**
     * Returns the target session's account StateID and a login context when the process is successful,
     * or null otherwise
     */
    suspend fun handleOAuthCode(state: String, code: String) {
        Log.i(logTag, "Handling OAuth response")

        _currIntent = KnownIntent.RE_LOG
        _processState.value = PreLaunchState.PROCESSING
        switchLoading(true)
        updateMessage("Retrieving authentication token...")

        viewModelScope.launch {

            delay(smoothActionDelay)
            val (accID, lc) =
                authService.handleOAuthResponse(accountService, sessionFactory, state, code)
                    ?: run {// Nothing to do, we simply ignore the call
                        _processState.value = PreLaunchState.SKIP
                        return@launch
                    }
            try {
                updateMessage("Updating account info...")
                _accountID.value = accID
                _loginContext.value = lc
                Log.e(logTag, "Updating account info: $accID -> $lc")
                delay(smoothActionDelay)
                accountService.refreshWorkspaceList(accID)

                _appState.value = AppState(
                    stateID = accID,
                    intentID = intentID,
                    // TODO maybe also insure here that we have a first correct destination.
                    route = null,
                    context = lc
                )
                _processState.value = PreLaunchState.DONE
            } catch (se: SDKException) {
                updateErrorMsg("Could not refresh workspaces for $accID: ${se.message}")
            }
        }
    }

    fun handleShare(clipData: ClipData) {
        Log.d(logTag, "ACTION_SEND received, clipData: $clipData")
        clipData.getItemAt(0).uri?.let {
            _uris.add(it)
        }
        _appState.value = AppState(
            StateID.NONE,
            intentID,
            ShareDestinations.ChooseAccount.route,
            null
        )
        _currIntent = KnownIntent.SHARE
        _processState.value = PreLaunchState.DONE
    }

    fun handleShares(clipData: ClipData) {
        Log.d(logTag, "ACTION_SEND_MULTI received, clipData: $clipData")
        for (i in 0 until clipData.itemCount) {
            clipData.getItemAt(i).uri?.let {
                _uris.add(it)
            }
        }
        _appState.value = AppState(
            StateID.NONE,
            intentID,
            ShareDestinations.ChooseAccount.route,
            null
        )
        _currIntent = KnownIntent.SHARE_MULTI
        _processState.value = PreLaunchState.DONE
    }

    fun shareAt(stateID: StateID) {
        // TODO also handle network state and preferences

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
        Log.i(logTag, "Cleared")
    }

    init {
        Log.i(logTag, "Created")
    }
}
