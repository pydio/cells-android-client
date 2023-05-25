package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Hold the session that is currently in foreground for browsing the cache and the remote server.
 */
class ConnectionService(
    private val coroutineService: CoroutineService,
    networkService: NetworkService,
    private val accountService: AccountService,
    private val appCredentialService: AppCredentialService,
) {

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "ConnectionService[${id.substring(24)}]"

    private val serviceScope = coroutineService.cellsIoScope

    private val networkStatusFlow = networkService.networkStatusFlow
    val sessionView: Flow<RSessionView?> = accountService.activeSessionViewF
    val sessionStatusFlow: Flow<SessionStatus> = getSessionFlow()
    val currAccountID: Flow<StateID?> =
        sessionView.map { it?.accountID?.let { accId -> StateID.fromId(accId) } }
    val customColor: Flow<String?> = sessionView.map { currSessionView ->
        currSessionView?.customColor()
    }

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

    private fun getSessionFlow(): Flow<SessionStatus> = sessionView
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
                if (newStatus == SessionStatus.CAPTIVE) {
                    // We do not change the status yet
                } else if (newStatus != SessionStatus.NO_INTERNET && !it.isReachable) {
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

    private var currJob: Job? = null
//    private val lock = Any()

    fun relaunchMonitoring() {
        serviceScope.launch {
            currJob?.cancelAndJoin()
            currJob = launch {
                while (true) {
                    monitorCredentials()
                    delay(10000)
                }
            }
        }
    }

    fun pauseMonitoring() {
        serviceScope.launch {
            currJob?.cancelAndJoin()
        }
        // TODO we should also pause the other hot flows ?
    }

    // TODO this must be improved
    private suspend fun monitorCredentials() = withContext(Dispatchers.IO) {
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

        Log.e(logTag, "Monitoring Credentials for $currID, found a token that needs refresh")
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

    init {
        Log.i(logTag, "### ConnectionService initialised")
    }
}
