package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.ErrorService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BrowseRemoteVM(
    private val coroutineService: CoroutineService,
    private val networkService: NetworkService,
    private val connectionService: ConnectionService,
    private val accountService: AccountService,
    private val nodeService: NodeService,
) : ViewModel(), KoinComponent {

    private val logTag = "BrowseRemoteVM"

    val loadingState = connectionService.loadingState
    val isLegacy = connectionService.isRemoteLegacy

    fun watch(newStateID: StateID, isForceRefresh: Boolean) {
        connectionService.setCurrentStateID(newStateID)
        if (isForceRefresh) {
            connectionService.forceRefresh()
        }
    }

    fun pause(oldID: StateID) {
        connectionService.pause(oldID)
    }

    private val errorService: ErrorService by inject()

    // Manage poll jobs
//    private var currWatcher: Job? = null
//    private var _isActive = false
//    private val backOffTicker = BackOffTicker()


    // UI State
//    private val _loadingState = MutableStateFlow(LoadingState.STARTING)


//    private val _errorMessageF = MutableStateFlow<ErrorMessage?>(null)

    // TODO reset backoff ticker when we pass from offline to connected.

    // We derive the Loading state to also expose Unreachable status to calling composables.
//    val loadingState: StateFlow<LoadingState> =
//        _loadingState.combine(connectionService.sessionStatusFlow) { state, status ->
//            Log.d(logTag, "Computing loading state with state: $state & status: $status")
//            if (SessionStatus.NO_INTERNET == status
//                || SessionStatus.SERVER_UNREACHABLE == status
//            ) {
//                LoadingState.SERVER_UNREACHABLE
//            } else {
//                state
//            }
//        }.stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = LoadingState.STARTING
//        )

//    private val _stateID = MutableStateFlow(StateID.NONE)
//    val stateID: StateFlow<StateID> = _stateID
//
//    // Expose a flag to the various screens to know if remote is Cells or P8
//    private var _isLegacy = false
//    val isLegacy: Boolean = _isLegacy
//
//    fun watch(newStateID: StateID, isForceRefresh: Boolean) {
//        viewModelScope.launch {
//            try {
//                accountService.getSession(newStateID)?.let {
//                    _isLegacy = it.isLegacy
//                }
////                delay(1000)
//                var msg = "Watching${if (isLegacy) " P8 server at" else ""} $newStateID "
//                if (isForceRefresh) msg += " (Force refresh)"
//                msg += "\n    Loading state: ${_loadingState.value}"
//                Log.e(logTag, msg)
//                _loadingState.value = when {
//                    !networkService.isConnected() -> return@launch // Do nothing
//                    isForceRefresh -> LoadingState.PROCESSING
//                    else -> LoadingState.STARTING
//                }
//                // FIXME use a specific cancellation exception that can be used to recatch the cancellation on line 144
//                currWatcher?.cancel(CellsCancellation())
//                _isActive = false
//                _stateID.value = newStateID
//                resume()
//            } catch (e: Exception) {
//                Log.e(logTag, "could not start watching, error: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//    }

//    fun pause(oldID: StateID) {
//        if (oldID == stateID.value) {
//            Log.i(logTag, "... Pause remote watching for [${stateID.value}]")
//            _isActive = false
//            _loadingState.value = LoadingState.IDLE
//        } else {
//            Log.d(logTag, "Received pause for $oldID but currID is  [${stateID.value}]")
//            Log.d(logTag, "Received pause for $oldID but currID is  [${stateID.value}]")
//        }
//    }
//
//    private fun resume() {
//        if (!_isActive) {
//            _isActive = true
//            currWatcher = watchFolder()
//        }
//        backOffTicker.resetIndex()
//        Log.i(logTag, "... Remote watching for [${stateID.value}] has been resumed")
//    }

    // Technical local objects
//    private fun watchFolder() = viewModelScope.launch {
//        loop@ while (_isActive) {
//            try {
//                doPull()
//                val nd = withContext(coroutineService.cpuDispatcher) {
//                    backOffTicker.getNextDelay()
//                }
//                delay(TimeUnit.SECONDS.toMillis(nd))
//                val msg = "Watching folders at ${stateID.value}"
//                if (_isActive) {
//                    Log.d(logTag, "$msg, next delay: ${nd}s")
//                } else {
//                    Log.d(logTag, "Stop $msg")
//                }
//            } catch (e: CancellationException) {
////                if (e is CellsCancellation || e.cause?.let { it is CellsCancellation } == true) {
////                    Log.d(logTag, "internal cancellation, doing nothing")
////                } else {
//                Log.w(logTag, "Pausing poll: ${e.message}")
//                pause(stateID.value)
////                }
//            } catch (e: Exception) {
//                Log.e(logTag, "Unexpected error: $e")
//                e.printStackTrace()
//                Log.e(logTag, "####################")
//            }
//        }
//    }

//    private suspend fun doPull() {
//        // TODO clean and add "Cancel feature"
//        var result: Pair<Int, String?> = Pair(0, "")
//
//        stateID.value.let {
//            if (loadingState.value == LoadingState.SERVER_UNREACHABLE) {
//                // TODO insure we do not want to display an error message to the end user
//                Log.w(logTag, "$it: server is unreachable")
//                pause(it)
//                return
//            } else if (StateID.NONE != it) {
//
//                // Manual retry when we come back on an account after a while, to give some time for the token to be refreshed
//
//                var retry = false
//                var retryNb = 0
//                while (retryNb == 0 || (retry && retryNb < 4)) {
//                    retryNb++
//                    try {
//                        result = if (Str.empty(it.file)) {
//                            accountService.refreshWorkspaceList(it.account())
//                        } else {
//                            nodeService.pull(it)
//                        }
//                    } catch (se: SDKException) {
//                        if (ErrorCodes.token_expired == se.code) {
//                            retry = true
//                            delay(1500)
//                        } else {
//                            throw se
//                        }
//                    }
//                }
//            }
//        }
//
//        if (Str.notEmpty(result.second)) {
//            Log.e(logTag, "Cannot refresh, msg: ${result.second!!}")
//            // errorService.appendError(result.second!!)
//            pause(stateID.value)
//        }
//        if (result.first > 0) { // At least one change => reset backoff ticker
//            backOffTicker.resetIndex()
//        }
//        _loadingState.value = LoadingState.IDLE
//    }

    init {
        Log.e(logTag, "#################################################")
        Log.e(logTag, "... Main browse view model has been initialised")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(logTag, "cleared")
    }
}


class CellsCancellation() : CancellationException()