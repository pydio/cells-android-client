package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.models.CellsCancellation
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Hold the session that is currently in foreground for browsing the cache and the remote server.
 */
class ConnectionService(
    private val coroutineService: CoroutineService,
    networkService: NetworkService,
    private val accountService: AccountService,
    private val nodeService: NodeService,
    private val appCredentialService: AppCredentialService,
) {

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "ConnectionService[${id.substring(24)}]"

    private val serviceScope = coroutineService.cellsIoScope

    private var currMonitoringCredentialsJob: Job? = null
    private var currPollJob: Job? = null

    // Cold flows
    val sessionView: Flow<RSessionView?> = accountService.activeSessionView

    val currAccountID: Flow<StateID?> =
        sessionView.map { it?.accountID?.let { accId -> StateID.fromId(accId) } }
    val customColor: Flow<String?> = sessionView.map { currSessionView ->
        currSessionView?.customColor()
    }

    // Expose a flag to the various screens to know if current remote is Cells or P8
    private var _isRemoteLegacy = false
    val isRemoteLegacy: Boolean = _isRemoteLegacy

    @OptIn(ExperimentalCoroutinesApi::class)
    val wss: Flow<List<RWorkspace>> = sessionView.flatMapLatest { currSessionView ->
        accountService.getWsByTypeFlow(
            SdkNames.WS_TYPE_DEFAULT,
            currSessionView?.accountID ?: StateID.NONE.id
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val cells: Flow<List<RWorkspace>> = sessionView.flatMapLatest { currSessionView ->
        accountService.getWsByTypeFlow(
            SdkNames.WS_TYPE_CELL,
            currSessionView?.accountID ?: StateID.NONE.id
        )
    }

    // Hot flows

    private val networkStatusFlow = networkService.networkStatusFlow
    private val loadingFlag = MutableStateFlow(LoadingState.IDLE)

    val sessionStatusFlow: Flow<SessionStatus> = sessionView
        .combine(networkStatusFlow) { activeSession, networkStatus ->

            var newStatus = when (networkStatus) {
                NetworkStatus.Unknown -> {
                    // TODO enhance this:
                    Log.e(logTag, "unexpected network type: $networkStatus")
                    SessionStatus.OK
                }

                NetworkStatus.Unmetered -> SessionStatus.OK
                NetworkStatus.Metered -> SessionStatus.METERED
                NetworkStatus.Roaming -> SessionStatus.ROAMING
                NetworkStatus.Unavailable -> SessionStatus.NO_INTERNET
                NetworkStatus.Captive -> SessionStatus.CAPTIVE
            }

            // We then refine based on the current foreground session
            activeSession?.let {

                // Update the helper flag
                _isRemoteLegacy = it.isLegacy

                if (newStatus == SessionStatus.CAPTIVE) {
                    // We do not change the status yet
                } else if (newStatus != SessionStatus.NO_INTERNET && !it.isReachable) {

                    // Request a refresh of the reachable status for current stateID
                    serviceScope.launch {
                        appCredentialService.insureServerIsReachable(it.getStateID())
                    }
                    // We have internet access but cannot ping the server
                    newStatus = SessionStatus.SERVER_UNREACHABLE
                }

                Log.i(logTag, "Got a Session view: ${it.getStateID()}, status: $newStatus")
                if (it.authStatus != AppNames.AUTH_STATUS_CONNECTED) {
                    pauseMonitoring()
                    newStatus = if (newStatus != SessionStatus.NO_INTERNET
                        && newStatus != SessionStatus.CAPTIVE
                        && newStatus != SessionStatus.SERVER_UNREACHABLE
                    ) {
                        SessionStatus.CAN_RELOG
                    } else {
                        // SessionStatus.NOT_LOGGED_IN
                        newStatus // We want to keep internet status rather than "not logged in"
                    }
                } else {
                    relaunchMonitoring()
                }
            } ?: run {
                Log.e(logTag, " **No** Session view...")
                newStatus = SessionStatus.SERVER_UNREACHABLE
            }
            newStatus
        }
        .flowOn(coroutineService.ioDispatcher)
        .conflate()

    val loadingState: StateFlow<LoadingState> =
        loadingFlag.combine(sessionStatusFlow) { state, status ->
            val currState = if (SessionStatus.NO_INTERNET == status
                || SessionStatus.SERVER_UNREACHABLE == status
            ) {
                LoadingState.SERVER_UNREACHABLE
            } else {
                state
            }
            Log.d(logTag, "## Loading flag: $state, session status: $status")
            Log.e(logTag, "\t=> Computed loading state: $currState")
            currState
        }.stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoadingState.STARTING
        )


    fun relaunchMonitoring() {
        relaunchCredJob()
        relaunchPollJob()
    }

    fun pauseMonitoring() {
        pauseCredJob()
        pausePollJob()
    }

    private fun relaunchPollJob() {
        serviceScope.launch {
            currPollJob?.cancelAndJoin()
            currPollJob = serviceScope.launch {
                Log.e(logTag, "### Launching Poll Job $this")
                while (true) {
                    Log.i(logTag, "## New watch iteration for $this")
                    watchFolder()
                }
            }
        }
    }

    private fun pausePollJob() {
        serviceScope.launch {
            currPollJob?.cancelAndJoin()
        }
    }

    private fun relaunchCredJob() {
        serviceScope.launch {
            currMonitoringCredentialsJob?.cancelAndJoin()
            currMonitoringCredentialsJob = launch {
                while (true) {
                    monitorCredentials()
                    delay(10000)
                }
            }
        }
    }

    private fun pauseCredJob() {
        serviceScope.launch {
            currMonitoringCredentialsJob?.cancelAndJoin()
        }
    }


    // TODO this must be improved
    private suspend fun monitorCredentials() = withContext(coroutineService.ioDispatcher) {
        val currSession = sessionView.last() ?: return@withContext
        val currID = currSession.getStateID()
        if (currSession.isLegacy) {
            // this is for Cells only
            return@withContext
        }
        val token = appCredentialService.get(currID.id) ?: run {
            Log.w(logTag, "Session $currID has no credentials, aborting")
            pauseMonitoring()
            return@withContext
        }
        if (token.expirationTime > (currentTimestamp() + 120)) {
            return@withContext
        }

        Log.w(logTag, "Monitoring Credentials for $currID, found a token that needs refresh")
        val expTimeStr = timestampToString(token.expirationTime, "dd/MM HH:mm")
        Log.d(logTag, "   Expiration time is $expTimeStr")
        val timeout = currentTimestamp() + 30
        val oldTs = token.expirationTime
        var newTs = oldTs

        appCredentialService.requestRefreshToken(currID)
        try {
            while (oldTs == newTs && currentTimestamp() < timeout) {
                delay(1000)
                appCredentialService.getToken(currID)?.let {
                    newTs = it.expirationTime
                }
            }
            return@withContext
        } catch (se: SDKException) {
            Log.e(logTag, "Unexpected error while monitoring refresh token process for $currID")
//            if (se.code == ErrorCodes.refresh_token_expired) {
//                // We cannot refresh anymore, aborting
//                return@withContext
//            }
//            return@withContext
        }
    }

    /* MANAGE POLLING */

    private var currStateID = StateID.NONE
    private var _isActive = false
    private var currWatcher: Job? = null
    private var delayJob: Job? = null
    private val backOffTicker = BackOffTicker()

    private fun setActive(active: Boolean) {
        _isActive = active
    }

    fun setCurrentStateID(newStateID: StateID) {
        if (newStateID != currStateID) {
            currStateID = newStateID
            setActive(true)
            backOffTicker.resetIndex()
            delayJob?.cancel(CellsCancellation())
        }
    }

    fun forceRefresh() {
        loadingFlag.value = LoadingState.PROCESSING
        backOffTicker.resetIndex()
        delayJob?.cancel(CellsCancellation())
    }

    fun pause(oldID: StateID) {
        if (oldID == currStateID) {
            Log.i(logTag, "... Pause remote watching for [${currStateID}]")
            setActive(false)
            loadingFlag.value = LoadingState.IDLE
        } else {
            Log.d(logTag, "Received pause for $oldID but currID is [${currStateID}]")
        }
    }

    private suspend fun watchFolder() {
        if (_isActive) {
            try {
                doPull(currStateID)
            } catch (e: Exception) {
                Log.e(logTag, "Unexpected error: $e")
                e.printStackTrace()
                Log.e(logTag, "####################")
            }
        }
        val nd = backOffTicker.getNextDelay()

        // Handle cancellable delay
        val tmpDelayJob = serviceScope.launch {
            try {
                Log.d(logTag, "\tPull done, about to sleep for $nd seconds\n\tserviceScope $this")
                delay(TimeUnit.SECONDS.toMillis(nd))
                val msg = "Watching folders at $currStateID - $this"
                if (_isActive) {
                    Log.d(logTag, "\t$msg \n\tStill running after ${nd}s delay")
                } else {
                    Log.d(logTag, "\t$msg \n\tStopped after ${nd}s delay")
                }
                //Log.e(logTag, "### After delay")
            } catch (e: CancellationException) {
                Log.w(logTag, "Delay has been cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e(logTag, "## Unexpected error for $this:\n\t$e")
                // e.printStackTrace()
            }
        }
        delayJob = tmpDelayJob
        tmpDelayJob.join()
        Log.d(logTag, "\tAfter join for ${delayJob.toString()}")
    }

    @Throws(SDKException::class)
    private suspend fun doPull(stateID: StateID) {

        var result: Pair<Int, String?> = Pair(0, "")

        if (loadingState.value == LoadingState.SERVER_UNREACHABLE) {
            // we do not try to pull but don't stop the main job
            return
        } else if (StateID.NONE == stateID) {
            // no state ID, we do not try to pull but don't stop the main job
            return
        } else {

            // Manual retry when we come back on an account after a while, to give some time for the token to be refreshed
            var retry = false
            var retryNb = 0
            while (retryNb == 0 || (retry && retryNb < 4)) {
                retryNb++
                try {
                    result = if (Str.empty(stateID.file)) {
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

        if (Str.notEmpty(result.second)) {
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
        Log.i(logTag, "### ConnectionService initialised")
    }
}
