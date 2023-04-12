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
import com.pydio.android.cells.utils.timestampToString
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
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
 * Hold the session that is currently in foreground for browsing the cache and the remote server.
 */
class ConnectionVM(
    networkService: NetworkService,
    private val accountService: AccountService,
    private val appCredentialService: AppCredentialService,
) : ViewModel() {

    enum class SessionStatus {
        NO_INTERNET, NOT_LOGGED_IN, CAN_RELOG, ROAMING, METERED, OK
    }

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "ConnectionVM[${id.substring(24)}]"

    private val liveNetwork = networkService.networkType

    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView
    val currAccountID: LiveData<StateID?> = sessionView.map { currSessionView ->
        currSessionView?.accountID?.let {
            StateID.fromId(it)
        }
    }

//    private val _cachedAccountID = MutableStateFlow<StateID?>(null)

    val customColor: LiveData<String?> = sessionView.map { currSessionView ->
//        // Kind of hack to insure we also restart the session monitoring when switch account
//        currSessionView?.accountID?.let {
//            val newStateID = StateID.fromId(it)
//            if (newStateID != _cachedAccountID.value) {
//                _cachedAccountID.value = newStateID
//                relaunchMonitoring()
//            }
//        }
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
                if (it.authStatus != AppNames.AUTH_STATUS_CONNECTED) {
                    pauseMonitoring()
                    newStatus = if (newStatus != SessionStatus.NO_INTERNET) {
                        SessionStatus.CAN_RELOG
                    } else {
                        // TODO refine, we have following sessions status:
                        // AUTH_STATUS_NEW, AUTH_STATUS_NO_CREDS, AUTH_STATUS_UNAUTHORIZED
                        // AUTH_STATUS_EXPIRED, AUTH_STATUS_REFRESHING, AUTH_STATUS_CONNECTED
                        SessionStatus.NOT_LOGGED_IN
                    }
                } else {
                    relaunchMonitoring()
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

    // TODO this must be improved
    private suspend fun monitorCredentials() = withContext(Dispatchers.IO) {
        val currSession = sessionView.value ?: return@withContext
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

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "### Connection VM cleared!")
    }

    init {
        Log.e(logTag, "### Connection VM initialised")
    }
}
