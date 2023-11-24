package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.ServerConnection
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.services.models.SessionState
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Hold the session that is currently in foreground for browsing the cache and the remote server.
 */
class ConnectionService(
    coroutineService: CoroutineService,
    networkService: NetworkService,
    private val appCredentialService: AppCredentialService,
    private val accountService: AccountService,
    private val pollService: PollService,
) {

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "ConnectionServ_${id.substring(30)}"

    private val serviceScope = coroutineService.cellsIoScope

    private val lock = Any()
    private var lastTimeChecked = -1L

    private val networkStatusFlow = networkService.networkStatusFlow
    private val loadingFlag = pollService.loadingFlag

    // Cold session view flow
    val sessionView: Flow<RSessionView?> = accountService.activeSessionView
    val customColor: Flow<String?> = sessionView.map { currSession ->
        currSession?.let {
            if (it.isReachable && !it.isLoggedIn()) {
                CellsColor.OfflineColor
            } else {
                it.customColor()
            }
        }
    }

    // val customColor: Flow<String?> = sessionView.combine(networkStatus) { currSession, networkStatus ->
    //         if (currSession == null) null else {
    //             when (serverConnectionState(SessionState.from(currSession, networkStatus))) {
    //                 ServerConnection.UNREACHABLE -> CellsColor.OfflineColor
    //                 ServerConnection.LIMITED -> CellsColor.MeteredColor
    //                 ServerConnection.OK -> currSession.customColor()
    //             }
    //         }
    //     }
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

    val sessionStateFlow: StateFlow<SessionState> = sessionView
        .combine(networkStatusFlow) { activeSession, networkStatus ->
            val nextState = if (activeSession == null) {
                SessionState(StateID.NONE, false, networkStatus, LoginStatus.Undefined)
            } else {
                var serverIsReachable = true
                if (networkStatus.isConnected() && !activeSession.isReachable) {
                    // We have internet access but current server is marked as un-reachable
                    // We try again to connect
                    serviceScope.launch {
                        // switch to unreachable until proven we can reach it (4 lines below)
                        serverIsReachable = false
                        delay(1000L) // Wait 1 sec before doing the request
                        val doIt: Boolean
                        synchronized(lock) {
                            if (currentTimestamp() - lastTimeChecked > 3) {
                                lastTimeChecked = currentTimestamp()
                                doIt = true
                            } else {
                                doIt = false
                            }
                        }
                        if (doIt) {
                            // This also update cached reachable status of the server
                            serverIsReachable =
                                appCredentialService.insureServerIsReachable(activeSession.getStateID())
                        }
                    }
                }

                if (activeSession.authStatus != LoginStatus.Connected.id) {
                    // TODO we do not pause yet monitoring when we have no connection or server is unreachable ??
                    pauseMonitoring()
                } else {
                    relaunchMonitoring()
                }
                SessionState(
                    activeSession.getStateID(),
                    activeSession.isReachable,
                    networkStatus,
                    LoginStatus.fromId(activeSession.authStatus)
                )
            }
            Log.e(logTag, ".... Emit new state: $nextState")
            nextState
        }.stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SessionState(
                accountID = StateID.NONE,
                networkStatus = NetworkStatus.UNKNOWN,
                isServerReachable = false,
                loginStatus = LoginStatus.Undefined
            )
        )

    // And direct derivatives
    val currAccountID: Flow<StateID> = sessionStateFlow.map { it.accountID }

    // Shortcut. Remove
    val isRemoteLegacy: Boolean = sessionStateFlow.value.isServerLegacy

    val liveConnectionState: StateFlow<ConnectionState> =
        loadingFlag.combine(sessionStateFlow) { loading, connection ->
            appliedConnectionState(loading, connection)
        }.stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ConnectionState(LoadingState.STARTING, ServerConnection.OK)
        )

    fun appliedConnectionState(
        loading: LoadingState,
        connection: SessionState
    ): ConnectionState {
        val tmpLoading =
            if (!connection.networkStatus.isConnected() || !connection.isServerReachable) {
                LoadingState.IDLE
            } else {
                loading
            }
        return ConnectionState(tmpLoading, serverConnectionState(connection))
    }

    // Shortcuts
    fun isConnected(): Boolean {
        return liveConnectionState.value.serverConnection != ServerConnection.UNREACHABLE
    }

    // Delegated to pollService
    fun relaunchMonitoring() {
        pollService.relaunchMonitoring()
    }

    fun pauseMonitoring() {
        pollService.pauseMonitoring()
    }

    fun setCurrentStateID(newStateID: StateID) {
        pollService.watch(newStateID)
    }

    fun forceRefresh() {
        pollService.forceRefresh()
    }

    fun pause(oldID: StateID) {
        pollService.pause(oldID)
    }

    // Debug only
    private var lastKnownConnection: ServerConnection? = null

    private fun serverConnectionState(connection: SessionState): ServerConnection {
        val newConn = if (
            !connection.networkStatus.isConnected()
            || !connection.isServerReachable
            || !connection.loginStatus.isConnected()
        ) {
            ServerConnection.UNREACHABLE
        } else if (
            connection.networkStatus == NetworkStatus.ROAMING
            || connection.networkStatus == NetworkStatus.METERED
        ) {
            // TODO also include preference checks for Offline and Roaming
            ServerConnection.LIMITED
        } else {
            ServerConnection.OK
        }
        // TODO remove dirty debug only
        if (newConn != lastKnownConnection) {
            Log.e(logTag, "New connection state: $newConn from connection: $connection")
            lastKnownConnection = newConn
        }
        return newConn
    }

    init {
        Log.i(logTag, "### ConnectionService initialised")
    }
}
