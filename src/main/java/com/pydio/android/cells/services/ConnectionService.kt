package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.ServerConnection
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
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

    // Current foreground session
    val sessionView: Flow<RSessionView?> = accountService.activeSessionView
    val currAccountID: Flow<StateID?> =
        sessionView.map { it?.accountID?.let { accId -> StateID.fromId(accId) } }
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

    private val networkStatusFlow = networkService.networkStatusFlow
    private val loadingFlag = pollService.loadingFlag
    private var lastTimeChecked = -1L
    private val lock = Any()

    val sessionStateFlow: StateFlow<SessionState> = sessionView
        .combine(networkStatusFlow) { activeSession, networkStatus ->

            if (activeSession == null) {
                SessionState(StateID.NONE, networkStatus, false, LoginStatus.Undefined)
            } else {

                if (networkStatus.isConnected() && !activeSession.isReachable) {
                    // We have internet access but current server is marked as un-reachable
                    // We try again to connect
                    serviceScope.launch {
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
                            appCredentialService.insureServerIsReachable(activeSession.getStateID())
                        }
                    }
                }

                if (activeSession.authStatus != LoginStatus.Connected.id) {
                    pauseMonitoring()
                } else {
                    relaunchMonitoring()
                }
                SessionState(
                    activeSession.getStateID(),
                    networkStatus,
                    activeSession.isReachable,
                    LoginStatus.fromId(activeSession.authStatus)
                )
            }
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

    fun isConnected(): Boolean {
        return liveConnectionState.value.serverConnection != ServerConnection.UNREACHABLE
    }

    fun relaunchMonitoring() {
        pollService.relaunchMonitoring()
    }

    fun pauseMonitoring() {
        pollService.pauseMonitoring()
    }

    /* MANAGE POLLING */

    private var _isActive = false


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

data class ConnectionState(val loading: LoadingState, val serverConnection: ServerConnection)

data class SessionState(
    val accountID: StateID,
    val networkStatus: NetworkStatus,
    val isServerReachable: Boolean,
    val loginStatus: LoginStatus,
    val isServerLegacy: Boolean = false
) {
    companion object {
        fun from(view: RSessionView, status: NetworkStatus): SessionState {
            return SessionState(
                accountID = view.getStateID(),
                networkStatus = status,
                isServerReachable = view.isReachable,
                isServerLegacy = view.isLegacy,
                loginStatus = LoginStatus.fromId(view.authStatus)
            )
        }
    }
}

fun SessionState.isOK(): Boolean {
    // TODO rather also rely on server connection to also take prefs limit in account
    return isServerReachable && networkStatus == NetworkStatus.OK && loginStatus.isConnected()
}
