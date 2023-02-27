package com.pydio.android.cells.services

import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SDKException.RemoteIOException
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.CredentialService
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent

/**
 * This app specific credential service provides a refresh method that insures that only
 * one refresh token process is launched at a time.
 */
class AppCredentialService(
    private val tokenStore: Store<Token>,
    passwordStore: Store<String>,
    private val networkService: NetworkService,
) : CredentialService(tokenStore, passwordStore), KoinComponent {

    private val logTag = AppCredentialService::class.simpleName

    // Semaphore for the refresh process.
    private val lock = Any()

    private val credServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + credServiceJob)

    // TODO insure this is a clean way to call suspending method from the *JAVA* parent class.
    override fun refreshToken(id: String, transport: Transport): Token? {

        val token = runBlocking(Dispatchers.IO) {
            Log.e(logTag, "Launched blocking refresh token for ${transport.id}")
            // First ping the server: we can use the refresh token only once.
            try {
                transport.server.serverURL.ping()
            } catch (e: Exception) {
                Log.e(
                    logTag,
                    "Could ping remote server, aborting refresh token for ${transport.id}: ${e.message}"
                )
                e.printStackTrace()
                return@runBlocking null
            }
            try {
                doRefreshToken(id, transport)
            } catch (e: Exception) {
                Log.e(logTag, "Could not refresh token for ${transport.id}: ${e.message}")
                e.printStackTrace()
                return@runBlocking null
            }
        }
        return token
    }

    private suspend fun doRefreshToken(id: String?, transport: Transport): Token {

        val state = StateID.fromId(id)
        var token: Token = tokenStore.get(state.id) ?: throw SDKException(
            ErrorCodes.no_token_available,
            "Cannot refresh unknown token $state"
        )

        val doing = try {
            launchRefreshIfNotYetInProcess(state, token, transport)
        } catch (e: SDKException) {
            throw SDKException(ErrorCodes.cannot_refresh_token, "Refresh token expired for $state")
        }
        Log.d(logTag, "### About to wait for refreshed token, explicitly launched: $doing")

        // Wait until token is refreshed
        val timeout = currentTimestamp() + 30
        while (token.isExpired && currentTimestamp() < timeout) {
            Log.d(logTag, "... still waiting")
            delay(500)
            token = tokenStore.get(id) ?: token
        }

        if (token.isExpired) {
            throw SDKException(
                ErrorCodes.cannot_refresh_token,
                "Time-out while waiting for new token for $state"
            )
        }
        Log.d(logTag, "### Got a new token, explicitly launched: $doing")
        // Token has been refreshed
        return token
    }

    private fun launchRefreshIfNotYetInProcess(
        state: StateID,
        token: Token,
        transport: Transport
    ): Boolean {

        // Sanity checks
        if (!getRefreshLock(state)) {
            return false
        }

        if (transport !is CellsTransport) {
            throw SDKException(
                ErrorCodes.internal_error,
                "OAuth refresh token is not implemented for P8, cannot handle refresh for $state"
            )
        }
        try {
            val newToken = transport.getRefreshedOAuthToken(token.refreshToken)
                ?: throw SDKException(
                    ErrorCodes.refresh_token_expired,
                    "No new token has been generated for $state"
                )
            put(state.id, newToken)
        } catch (re: RemoteIOException) {
            Log.e(logTag, "Could not refresh token: ${re.message}. Aborting")
            return false
        } catch (se: SDKException) {
            if (se.code == ErrorCodes.refresh_token_expired) {
                // could not refresh, finally deleting referential to avoid being stuck
                // Log.e(logTag, "refresh_token_expired for $state")
                // Log.d(logTag, "Printing stack trace to understand where we come from:")
                // se.printStackTrace()
                // Log.e(logTag, "... and deleting credentials")
                remove(state.id)
                throw se
            }
            // throw se
            return false
        }
        return true
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
