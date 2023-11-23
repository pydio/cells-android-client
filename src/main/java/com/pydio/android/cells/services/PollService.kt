package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.android.cells.utils.CellsCancellation
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Performs background synchronisation depending on the folder that is currently shown to the end user.
 */
@OptIn(FlowPreview::class)
class PollService(
    coroutineService: CoroutineService,
    networkService: NetworkService,
    private val nodeService: NodeService,
    private val accountService: AccountService,
) {
    private val id: String = UUID.randomUUID().toString()
    private val logTag = "PollService_${id.substring(30)}"

    private val serviceScope = coroutineService.cellsIoScope

    private var pollJob: Job? = null

    private val _currStateID: MutableStateFlow<StateID> = MutableStateFlow(StateID.NONE)
    private val loadingFlag = MutableStateFlow(LoadingState.IDLE)

    private var wasActive = false
    private var currStateID = StateID.NONE

    private var _isActive = false
    private var delayJob: Job? = null
    private val backOffTicker = BackOffTicker()

    fun forceRefresh() {
        setActive(true)
        backOffTicker.resetIndex()
        delayJob?.cancel(CellsCancellation())
    }

    fun watch(stateID: StateID) {
        _currStateID.value = stateID
    }

    fun pause(oldID: StateID) {

        if (oldID == currStateID) {
            Log.i(logTag, "... Pause remote watching for [${currStateID}]")
            setActive(false)
            loadingFlag.value = LoadingState.IDLE
        } else {
            Log.d(logTag, "Received pause for [$oldID] but currID is [${currStateID}]")
        }
    }

    private fun handleNetworkChange(networkStatus: NetworkStatus, stateID: StateID) {
        setCurrentStateID(stateID)
        when (networkStatus) {
            NetworkStatus.UNAVAILABLE -> {
                Log.w(logTag, "... Lost connection to the internet, pausing poll")
                pausePollJob()
                wasActive = false
            }

            else -> {
                if (!wasActive) {
                    Log.i(logTag, "... We are online again, relaunching credential flows")
                    relaunchPollJob()
                }
                wasActive = true
            }
        }
    }

    fun relaunchMonitoring() {
        // Only relaunch if no job is referenced
        pollJob ?: run {
            relaunchPollJob()
        }
    }

    fun pauseMonitoring() {
        pausePollJob()
    }

    private fun relaunchPollJob() {
        serviceScope.launch {
            pollJob?.cancelAndJoin()
            pollJob = serviceScope.launch {
                Log.e(logTag, "####################################################")
                Log.e(logTag, "### New PollJob: ${this.hashCode()}")
                while (this.isActive) {
                    watchFolder(this)
                    delay(2000L)
                }
                Log.e(logTag, "####################################################")
                Log.e(logTag, "### Done Job ${this.hashCode()} (was active: ${this.isActive}")
            }
            Log.i(logTag, "### Relaunched PollJob, new ID: $pollJob")
        }
    }

    private fun pausePollJob() {
        serviceScope.launch {
            pollJob?.let {
                it.cancelAndJoin()
                Log.i(logTag, "### PollJob paused, ID was: $it")
                pollJob = null
            }
        }
    }

    private fun setCurrentStateID(newStateID: StateID) {
        if (newStateID != currStateID) {
            currStateID = newStateID
            setActive(true)
            loadingFlag.value = LoadingState.STARTING
            backOffTicker.resetIndex()
            delayJob?.cancel(CellsCancellation())
        }
    }

//    private fun unPausePoll(newStateID: StateID) {
//        setActive(true)
//        if (newStateID != currStateID) {
//            delayJob?.cancel(CellsCancellation())
//            loadingFlag.value = LoadingState.STARTING
//            currStateID = newStateID
//            backOffTicker.resetIndex()
//        }
//    }
//
//    private fun launchPollJob(): Job = serviceScope.launch {
//        Log.e(logTag, "####################################################")
//        Log.e(logTag, "### New PollJob: ${this.hashCode()}")
//        while (this.isActive) {
//            watchFolder(this)
//            delay(2000L)
//        }
//        Log.e(logTag, "####################################################")
//        Log.e(logTag, "### Done Job ${this.hashCode()} (was active: ${this.isActive}")
//    }

    private fun setActive(active: Boolean) {
        if (!active) {
            Log.e(logTag, "#### Setting _isActive to false !!")
        }
        _isActive = active
    }

    private suspend fun watchFolder(crScope: CoroutineScope) {
        try {
            doPull(currStateID, _isActive)
        } catch (e: Exception) {
            Log.e(logTag, "Unexpected error: $e")
            e.printStackTrace()
        }
        val nd = backOffTicker.getNextDelay()

        // Handle cancellable delay
        if (!crScope.isActive) {
            return
        }
        val tmpDelayJob = serviceScope.launch {
            try {
                delay(nd * 1000L)
                var msg = "\tafter ${nd}s sleep,"
                msg += if (_isActive) {
                    " still"
                } else {
                    " STOPPED"
                }
                msg += " watching at $currStateID - $this"
                Log.d(logTag, msg)
            } catch (e: CancellationException) {
                // Expected when we browse as the thread is waiting for next tick.
                Log.d(logTag, "Delay has been cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e(logTag, "## Unexpected error for $this:\n\t$e")
                // e.printStackTrace()
            }
        }
        delayJob = tmpDelayJob
        tmpDelayJob.join()
    }

    @Throws(SDKException::class)
    private suspend fun doPull(stateID: StateID, isActive: Boolean) {

        var result: Pair<Int, String?> = Pair(0, "")

        // FIXME re-handle this
//        if (liveConnectionState.value.serverConnection == ServerConnection.UNREACHABLE) {
//            serviceScope.launch {
//                // We trigger a ping to the server to check if it is back on-line
//                if (appCredentialService.insureServerIsReachable(stateID)) {
//                    // In such case we reset the ticker
//                    backOffTicker.resetIndex()
//                } else {
//                    // loadingFlag.value = LoadingState.IDLE
//                }
//            }
//            // we do not try to pull but don't stop the main job
//            return
//        } else
//
        if (StateID.NONE == stateID) {
            // no state ID, we do not try to pull but don't stop the main job
            return
        } else if (!isActive) {
            // The job has been paused, but we still keep on waiting for a state change with backoff timer
            // loadingFlag.value = LoadingState.IDLE
            return
        } else {

            // Manual retry when we come back on an account after a while, to give some time for the token to be refreshed
            var retry = false
            var retryNb = 0
            while (retryNb == 0 || (retry && retryNb < 4)) {
                retryNb++
                try {
                    result = if (stateID.file.isNullOrEmpty()) {
                        accountService.refreshWorkspaceList(stateID.account())
                    } else {
                        nodeService.pull(stateID)
                    }
                } catch (se: SDKException) {
                    if (ErrorCodes.token_expired == se.code) {
                        retry = true
                        delay(1500)
                    } else {
                        throw se
                    }
                }
            }
        }

        if (!result.second.isNullOrEmpty()) {
            Log.e(logTag, "Cannot refresh, msg: ${result.second!!}")
            // errorService.appendError(result.second!!)
            pause(stateID)
        }
        if (result.first > 0) { // At least one change => reset backoff ticker
            backOffTicker.resetIndex()
        }
        loadingFlag.value = LoadingState.IDLE
    }

    init {
        serviceScope.launch {
            // We also network status changes
            networkService.networkStatusFlow
                .debounce(1000L)
                .combine(_currStateID.debounce(100L)) { a, b -> a to b }
                .debounce(100L)
                .collect { (n, s) ->
                    handleNetworkChange(n, s)
                }
        }
        Log.i(logTag, "... PollService initialised")
    }
}
