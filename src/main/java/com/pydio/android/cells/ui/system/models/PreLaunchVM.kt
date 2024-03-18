package com.pydio.android.cells.ui.system.models

import android.content.ClipData
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.models.AppState
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
    NEW, PROCESSING, DONE, SKIP, TERMINATE, ERROR
}

enum class KnownIntent {
    LAUNCH, RE_LOG, SHARE, SHARE_MULTI
}

class PreLaunchVM(
//    private val prefs: PreferencesService,
//    coroutineService: CoroutineService,
    private val jobService: JobService,
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val accountService: AccountService,
    private val transferService: TransferService,
    private val intentID: String
) : ViewModel() {

    private val logTag = "PreLaunchVM"
    private val smoothActionDelay = 800L

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    // private val ioDispatcher = coroutineService.ioDispatcher
    // private val uiDispatcher = coroutineService.uiDispatcher

    private var _currIntent: KnownIntent = KnownIntent.LAUNCH
    // val currIntent = _currIntent

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

    private val _message = MutableStateFlow("")
    val message: StateFlow<String?> = _message.asStateFlow()

    private var _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
    suspend fun isAuthStateValid(state: String): Boolean {
        return authService.isAuthStateValid(state)
    }

    /**
     * Returns the target session's account StateID and a login context when the process is successful,
     * or null otherwise
     */
    suspend fun handleOAuthCode(state: String, code: String) {
        Log.i(logTag, "... Handling OAuth response")

        _currIntent = KnownIntent.RE_LOG
        _processState.value = PreLaunchState.PROCESSING
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
//                _accountID.value = accID
//                _loginContext.value = lc
                delay(smoothActionDelay)
                accountService.refreshWorkspaceList(accID)

                if (lc == AuthService.LOGIN_CONTEXT_SHARE) {
                    _processState.value = PreLaunchState.TERMINATE
//                } else if (lc == AuthService.LOGIN_CONTEXT_BROWSE) {
//                    _processState.value = PreLaunchState.TERMINATE
                } else {
                    _appState.value = AppState(
                        stateID = accID,
                        intentID = intentID,
                        // TODO maybe also insure here that we have a first correct destination.
                        route = null,
                        context = lc
                    )
                    _processState.value = PreLaunchState.DONE
                }
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
        launchPost(stateID = stateID, uris = uris) { jobID ->
            val route = ShareDestinations.UploadInProgress.createRoute(stateID, jobID)
            _appState.value = AppState(stateID, intentID, route, null)
        }
    }

    private fun launchPost(stateID: StateID, uris: List<Uri>, postLaunched: (Long) -> Unit) {
        val ids: MutableMap<Long, Pair<String, Uri>> = HashMap()
        viewModelScope.launch {
            // Register the parent Job
            val jobID = jobService.create(
                owner = AppNames.JOB_OWNER_USER,
                template = AppNames.JOB_TEMPLATE_SHARE,
                label = "Upload ${uris.size} files at $stateID",
                maxSteps = uris.size.toLong()
            )

            // Register the uploads
            for (uri in uris) {
                Log.i(logTag, "... Processing $uri ")
                try {
                    val tid = transferService.register(cr, uri, stateID, jobID)
                    ids[tid.first] = Pair(tid.second, uri)
                } catch (e: Exception) {
                    // TODO handle this
                }
            }

            // Mark the job has started
            jobService.launched(jobID)
            ids.forEach { currID ->
                // Launch the 2 steps process: local copy and then upload
                val (currName, currUri) = currID.value
                transferService.launchCopy(cr, currUri, stateID, currID.key, currName)?.let {
                    launch {
                        try {
                            transferService.uploadOne(it)
                            Log.w(logTag, "... $it ==> upload DONE")
                        } catch (e: Exception) {
                            jobService.failed(
                                jobID, e.message
                                    ?: "Unexpected error during upload of $currID at $stateID"
                            )
                            Log.e(logTag, "... $it ==> upload FAILED: ${e.message}")
                        }
                    }
                    Log.w(logTag, "... $it ==> upload LAUNCHED")
                } ?: run {
                    // TODO better error management
                    Log.e(logTag, "could not upload $currName at $stateID")
                    jobService.failed(jobID, "Could not launch copy for $currName")
                }
            }
            postLaunched(jobID)
        }
    }

    // UI Methods
    private suspend fun updateMessage(msg: String) {
        withContext(Dispatchers.Main) {
            _message.value = msg
        }
    }

    private suspend fun updateErrorMsg(msg: String) {
        withContext(Dispatchers.Main) {
            _errorMessage.value = msg
            _message.value = ""
            _processState.value = PreLaunchState.ERROR
        }
    }

    override fun onCleared() {
        Log.i(logTag, "... Cleared")
    }

    init {
        Log.i(logTag, "... Created")
    }

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

}
