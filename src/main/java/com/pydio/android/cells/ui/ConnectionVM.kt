package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AppCredentialService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


/**
 * Hold the session that is currently in foreground for browsing the cache
 * and the remote server.
 */
class ConnectionVM(
    networkService: NetworkService,
    private val accountService: AccountService,
    private val appCredentialService: AppCredentialService,
) : ViewModel() {

    private val id: String = UUID.randomUUID().toString()

    enum class SessionStatus {
        NO_INTERNET, NOT_LOGGED_IN, CAN_RELOG, ROAMING, METERED, OK
    }

    private val logTag = "ConnectionVM[${id.substring(24)}]"

    private val liveNetwork = networkService.networkType

    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView
    val currAccountID: LiveData<StateID?> = sessionView.map { currSessionView ->
        currSessionView?.accountID?.let { StateID.fromId(it) }
    }
    val customColor: LiveData<String?> = sessionView.map { currSessionView ->
        currSessionView?.customColor()
    }

    val wss: LiveData<List<RWorkspace>>
        get() = sessionView.switchMap { currSessionView ->
            accountService.getLiveWsByType(
                SdkNames.WS_TYPE_DEFAULT,
                currSessionView?.accountID ?: StateID.NONE.id
            )
        }
    val cells: LiveData<List<RWorkspace>>
        get() = sessionView.switchMap { currSessionView ->
            accountService.getLiveWsByType(
                SdkNames.WS_TYPE_CELL,
                currSessionView?.accountID ?: StateID.NONE.id
            )
        }

    val sessionStatusFlow: Flow<SessionStatus>
        get() = getSessionFlow()

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


    init {
        Log.e(logTag, " ### Initialised")
    }


    private var currJob: Job? = null

    fun relaunchMonitoring() {
        viewModelScope.launch {
            currJob?.cancelAndJoin()
            currJob = viewModelScope.launch {
                while (true) {
                    monitorCredentials()
                    delay(10000)
                }
            }
        }
    }

    fun pauseMonitoring() {
        viewModelScope.launch {
            currJob?.cancelAndJoin()
        }
        // TODO we should also pause the other LiveData and flows 
    }

    private suspend fun monitorCredentials(): Token? = withContext(Dispatchers.IO) {
        val currSession = sessionView.value ?: return@withContext null
        val currID = currSession.getStateID()
        if (currSession.isLegacy) {
            // this is for Cells only
            return@withContext null
        }
        val tmpTransport = accountService.getTransport(currSession.getStateID()) ?: run {
            Log.w(logTag, "Cannot monitor credentials with no transport for $currID")
            return@withContext null
        }
        val transport = tmpTransport as CellsTransport
        val token = appCredentialService.get(currID.id) ?: run {
            Log.w(logTag, "Cannot session with no credentials for $currID")
            return@withContext null
        }

        if (token.expirationTime > (currentTimestamp() + 120)) {
            return@withContext null
        }

        val timeout = currentTimestamp() + 30
        var newToken: Token? = null
        try {
            while (newToken == null && currentTimestamp() < timeout) {
                newToken = appCredentialService
                    .doRefreshToken(currID, token, currSession, transport)
                newToken ?: run { delay(1000) }
            }
            return@withContext newToken
        } catch (se: SDKException) {
            if (se.code == ErrorCodes.refresh_token_expired) {
                // We cannot refresh anymore, aborting
                return@withContext null
            }
            return@withContext null
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "### Connection VM cleared!!! ")
    }

    init {
        Log.e(logTag, "### Connection VM initialised")
    }
}
