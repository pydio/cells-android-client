package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NetworkService
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import java.util.*

/**
 * Hold the session that is currently in foreground for browsing the cache
 * and the remote server.
 */
class ConnectionVM(
    private val networkService: NetworkService,
    private val accountService: AccountService,
    id: String = UUID.randomUUID().toString(),
) : ViewModel() {

    private val logTag = "ConnectionVM[${id.substring(24)}]"

    enum class SessionStatus {
        NO_INTERNET, NOT_LOGGED_IN, CAN_RELOG, ROAMING, METERED, OK
    }

    private val liveNetwork = networkService.networkType

    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView
    val currAccountID: LiveData<StateID?>
        get() = Transformations.map(sessionView) { currSessionView ->
            currSessionView?.accountID?.let { StateID.fromId(it) }
        }
    val sessionStatusFlow: Flow<SessionStatus>
        get() = getSessionFlow()

    val wss: LiveData<List<RWorkspace>>
        get() = Transformations.switchMap(
            sessionView
        ) { currSessionView ->
            accountService.getLiveWsByType(
                SdkNames.WS_TYPE_DEFAULT,
                currSessionView?.accountID ?: Transport.UNDEFINED_STATE
            )
        }

    val cells: LiveData<List<RWorkspace>>
        get() = Transformations.switchMap(
            sessionView
        ) { currSessionView ->
            accountService.getLiveWsByType(
                SdkNames.WS_TYPE_CELL,
                currSessionView?.accountID ?: Transport.UNDEFINED_STATE
            )
        }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "### After onCleared()")
    }

    private fun getSessionFlow(): Flow<SessionStatus> = sessionView.asFlow()
        .combine(liveNetwork.asFlow()) { activeSession, currType ->
            Log.d(logTag, "Executing switch map with network type: $currType")
            // we first check the network type
            var newStatus = when (currType) {
                AppNames.NETWORK_TYPE_UNKNOWN,
                AppNames.NETWORK_TYPE_UNAVAILABLE -> {
                    // TODO enhance this:
                    Log.e(logTag, "unexpected network type: $currType")
                    SessionStatus.OK
                }
                AppNames.NETWORK_TYPE_UNMETERED -> SessionStatus.OK
                AppNames.NETWORK_TYPE_METERED -> SessionStatus.METERED
                AppNames.NETWORK_TYPE_ROAMING -> SessionStatus.ROAMING
                else -> SessionStatus.NO_INTERNET
            }

            // We then refine based on the current foreground session
            Log.d(logTag, " Got a status: $newStatus")
            activeSession?.let {
                Log.e(logTag, " Got a Session view: ${it.getStateID()}")
                if (it.authStatus != AppNames.AUTH_STATUS_CONNECTED
                ) {
                    newStatus = if (newStatus != SessionStatus.NO_INTERNET) {
                        SessionStatus.CAN_RELOG
                    } else {
                        // TODO refine, we have following sessions status:
                        // AUTH_STATUS_NEW, AUTH_STATUS_NO_CREDS, AUTH_STATUS_UNAUTHORIZED
                        // AUTH_STATUS_EXPIRED, AUTH_STATUS_REFRESHING, AUTH_STATUS_CONNECTED
                        SessionStatus.NOT_LOGGED_IN
                    }
                }
            } ?: run {
                Log.e(logTag, " **No** Session view...")
            }
            newStatus
        }
        .flowOn(Dispatchers.Default)
        .conflate()
}
