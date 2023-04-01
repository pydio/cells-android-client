package com.pydio.android.cells.services

import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.SessionViewDao
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SDKException.RemoteIOException
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.CredentialService
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

/**
 * This app specific credential service provides a refresh method that insures that only
 * one refresh token process is launched at a time.
 */
class AppCredentialService(
    private val tokenStore: Store<Token>,
    passwordStore: Store<String>,
    private val networkService: NetworkService,
    private val sessionViewDao: SessionViewDao,
) : CredentialService(tokenStore, passwordStore), KoinComponent {

    private val logTag = "AppCredentialService"

//    private val tokens: MutableMap<StateID, LoginStatus> = mutableMapOf()

    // Semaphore for the refresh process.
    private val lock = Any()

    private val credServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + credServiceJob)

    /**
     * This fire and forget a refresh token request if:
     * - remote is Cells
     * - we have all that we need
     * - no refresh is already running
     */
    override fun refreshToken(stateID: StateID, transport: Transport) {
        if (transport !is CellsTransport) {
            return
        } else serviceScope.launch {
            safelyRequestRefreshToken(stateID, transport)
        }
    }

//    private suspend fun requireRefreshFor(stateID: StateID, cellsTransport: CellsTransport) = withContext(Dispatchers.IO) {
//        tokens[stateID]?.let {
//            when (it) {
//                LoginStatus.Refreshing
//                -> return@withContext // already asked, simply ignore
//                else -> {
//                    tokens[stateID] = LoginStatus.Refreshing
//                    // TODO
////                    safelyRefreshToken(stateID)
//                }
//            }
//        } ?: run {
//            tokens[stateID] = LoginStatus.Refreshing
////            safelyRefreshToken(stateID)
//        }
//    }


    suspend fun safelyRequestRefreshToken(stateID: StateID, transport: CellsTransport): Token? {

        var token: Token = tokenStore.get(stateID.id) ?: throw SDKException(
            ErrorCodes.no_token_available,
            "Cannot refresh unknown token $stateID"
        )

        val session = sessionViewDao.getSession(stateID.accountId) ?: throw SDKException(
            ErrorCodes.no_token_available,
            "Cannot refresh unknown session $stateID"
        )
        return doRefreshToken(stateID, token, session, transport)
    }


    suspend fun doRefreshToken(
        stateID: StateID,
        token: Token,
        session: RSessionView,
        transport: CellsTransport
    ): Token? {

        Log.e(logTag, "Launching refresh token for $stateID")
        // First ping the server: we can use the refresh token only once.
        val serverURL = ServerURLImpl.fromAddress(session.url, session.skipVerify())
        try {
            serverURL.ping()
        } catch (e: Exception) {
            throw SDKException(
                ErrorCodes.unreachable_host,
                "Could not ping remote at $serverURL, aborting refresh process. ${e.message}"
            )
        }

        val newToken = launchRefreshIfNotYetInProcess(stateID, token, transport) ?: run {
            // Unsuccessful process but we can start again, otherwise an exception is launched
            return null
        }

        return newToken

//        val doing = try {
//        } catch (e: SDKException) {
//            throw SDKException(
//                ErrorCodes.cannot_refresh_token,
//                "Refresh token expired for $stateID"
//            )
//        }
//        Log.d(logTag, "### About to wait for refreshed token, explicitly launched: $doing")
//
//        // Wait until token is refreshed
//        val timeout = currentTimestamp() + 30
//        while (token.isExpired && currentTimestamp() < timeout) {
//            Log.d(logTag, "... still waiting")
//            delay(500)
//            token = tokenStore.get(id) ?: token
//        }
//
//        if (token.isExpired) {
//            throw SDKException(
//                ErrorCodes.cannot_refresh_token,
//                "Time-out while waiting for new token for $stateID"
//            )
//        }
//        Log.d(logTag, "### Got a new token, explicitly launched: $doing")
//        // Token has been refreshed
//        return token
    }

    private fun launchRefreshIfNotYetInProcess(
        stateID: StateID,
        token: Token,
        transport: CellsTransport
    ): Token? {

        // Sanity checks
        if (!getRefreshLock(stateID)) {
            Log.e(logTag, "Could not get lock to refresh token for $stateID. Aborting")
            return null
        }

        try {
            val newToken = transport.getRefreshedOAuthToken(token.refreshToken)
                ?: run {
                    Log.e(logTag, "Refresh token process returned null without throwing an error.")
                    Log.e(logTag, "Keep old credentials and abort.")
                    return null
                }
            put(stateID.id, newToken)
            return newToken
        } catch (re: RemoteIOException) {
            Log.e(logTag, "Could not refresh for $stateID. Still keeping old credentials")
            Log.e(logTag, "  Cause: remoteIOException: ${re.message}")
            return null
        } catch (se: Exception) {

            if (se is SDKException && se.code == ErrorCodes.refresh_token_expired) {
                // Could not refresh, finally deleting referential to avoid being stuck
                Log.e(logTag, "#######################")
                Log.e(logTag, "### Refresh token expired for $stateID")
                Log.e(logTag, "  Cause: ${se.message}")
                Log.e(logTag, "  !! Removing legacy credentials !! ")

                // Log.e(logTag, "refresh_token_expired for $state")
                // Log.d(logTag, "Printing stack trace to understand where we come from:")
                // Log.e(logTag, "... and deleting credentials")
                remove(stateID.id)
                throw se
            }
            Log.e(logTag, "Could not refresh for $stateID, unexpected exception: ${se.message}")
            se.printStackTrace()
            Log.e(logTag, "Keep old credentials and abort.")
            return null
        }
    }

    private fun getRefreshLock(state: StateID): Boolean {
        synchronized(lock) {
            // We re-query the DB to insure we have the latest version
            var token: Token = tokenStore.get(state.id) ?: throw SDKException(
                ErrorCodes.no_token_available,
                "Cannot refresh unknown token $state"
            )
            if (token.refreshingSinceTs > 0) {
                // TODO handle a timeout
                return false
            }

            // Sanity checks
            if (!networkService.isConnected()) {
                throw SDKException(
                    ErrorCodes.no_internet,
                    "Cannot refresh token $state with no access to the remote server"
                )
            }

            if (Str.empty(token.refreshToken)) {
                throw SDKException(
                    ErrorCodes.refresh_token_expired,
                    "No refresh token available for $state, cannot refresh"
                )
            }

            token.refreshingSinceTs = currentTimestamp()
            put(state.id, token)
        }
        return true
    }
}
