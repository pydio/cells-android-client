package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.ErrorService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class BrowseRemoteVM(
    private val coroutineService: CoroutineService,
    private val networkService: NetworkService,
    connectionService: ConnectionService,
    private val accountService: AccountService,
    private val nodeService: NodeService,
) : ViewModel(), KoinComponent {

    private val logTag = "BrowseRemoteVM"

    private val errorService: ErrorService by inject()

    // Manage poll jobs
    private var currWatcher: Job? = null
    private var _isActive = false
    private val backOffTicker = BackOffTicker()

    // UI State
    private val _loadingState = MutableStateFlow(LoadingState.STARTING)
//    private val _errorMessageF = MutableStateFlow<ErrorMessage?>(null)

    // TODO reset backoff ticker when we pass from offline to connected.

    // We derive the Loading state to also expose Unreachable status to calling composables.
    val loadingState: StateFlow<LoadingState> =
        _loadingState.combine(connectionService.sessionStatusFlow) { state, status ->
            Log.d(logTag, "Computing loading state with state: $state & status: $status")
            if (SessionStatus.NO_INTERNET == status
                || SessionStatus.SERVER_UNREACHABLE == status
            ) {
                LoadingState.SERVER_UNREACHABLE
            } else {
                state
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoadingState.STARTING
        )

    private val _stateID = MutableStateFlow(StateID.NONE)
    val stateID: StateFlow<StateID> = _stateID.asStateFlow()

    init {
        if (stateID.value != StateID.NONE) {
            _loadingState.value = LoadingState.STARTING
            Log.e(logTag, "####################################")
            Log.e(logTag, "####################################")
            Log.e(logTag, "####################################")
            Log.e(logTag, "####################################")
            Log.e(logTag, "... Starting for ${stateID.value}")
        }
    }

    fun watch(newStateID: StateID, isForceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                delay(1000)
                Log.e(
                    logTag,
                    "Watching $newStateID ${if (isForceRefresh) "(Force refresh)" else ""}"
                )
                Log.e(logTag, "    Loading state: ${_loadingState.value}")
                _loadingState.value = when {
                    !networkService.isConnected() -> return@launch // Do nothing
                    isForceRefresh -> LoadingState.PROCESSING
                    else -> LoadingState.STARTING
                }
                currWatcher?.cancel()
                _isActive = false
                _stateID.value = newStateID
                resume()
            } catch (e: Exception) {
                Log.e(logTag, "could not start watching, error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        Log.i(logTag, "... Pause remote watching for ${stateID.value}")
        _isActive = false
        _loadingState.value = LoadingState.IDLE
    }

    private fun resume() {
        if (!_isActive) {
            _isActive = true
            currWatcher = watchFolder()
        }
        backOffTicker.resetIndex()
        Log.i(logTag, "... Remote watching for ${stateID.value} has been resumed")
    }

    // Technical local objects
    private fun watchFolder() = viewModelScope.launch {
        loop@ while (_isActive) {
            try {
                doPull()
                val nd = withContext(coroutineService.cpuDispatcher) {
                    backOffTicker.getNextDelay()
                }
                delay(TimeUnit.SECONDS.toMillis(nd))
                val msg = "Watching folders at ${stateID.value}"
                if (_isActive) {
                    Log.d(logTag, "$msg, next delay: ${nd}s")
                } else {
                    Log.d(logTag, "Stop $msg")
                }
            } catch (e: CancellationException) {
                Log.w(logTag, "Pausing poll: ${e.message}")
                pause()
            } catch (e: Exception) {
                Log.e(logTag, "Unexpected error: $e")
                e.printStackTrace()
                Log.e(logTag, "####################")
            }
        }
    }

    private suspend fun doPull() {
        // TODO clean and add "Cancel feature"
        var result: Pair<Int, String?> = Pair(0, "")

        stateID.value.let {
            if (loadingState.value == LoadingState.SERVER_UNREACHABLE) {
                // TODO insure we do not want to display an error message to the end user
                Log.w(logTag, "$it: server is unreachable")
                pause()
                return
            } else if (StateID.NONE != it) {
                result = if (Str.empty(it.file)) {
                    accountService.refreshWorkspaceList(it.account())
                } else {
                    nodeService.pull(it)
                }
            }
        }

        if (Str.notEmpty(result.second)) {
            errorService.appendError(result.second!!)
            pause()
        }
        if (result.first > 0) { // At least one change => reset backoff ticker
            backOffTicker.resetIndex()
        }
        _loadingState.value = LoadingState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(logTag, "cleared")
    }
}
