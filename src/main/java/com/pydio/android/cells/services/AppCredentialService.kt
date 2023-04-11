package com.pydio.android.cells.services

import android.util.Log
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
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

/**
 * This app specific credential service provides a refresh method that insures that only
 * one refresh token process is launched at a time.
 */
class AppCredentialService(
    private val tokenStore: Store<Token>,
    passwordStore: Store<String>,
    private val transportStore: Store<Transport>,
    private val ioDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val sessionViewDao: SessionViewDao,
) : CredentialService(tokenStore, passwordStore), KoinComponent {

    private val logTag = "AppCredentialService"

    // Insure we only process one refresh request at a time
    private val requestRefreshChannel = Channel<StateID>()

    // Semaphore for the refresh process.
    private val lock = Any()

    private val credServiceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(ioDispatcher + credServiceJob)

    init {
        serviceScope.launch {
            try {
                withContext(ioDispatcher) {
                    while (this.coroutineContext.isActive) {
                        safelyRefreshToken(requestRefreshChannel.receive())
                    }
                    Log.e(logTag, "refresh channel has been cancelled")
                }
            } catch (e: Exception) {
                Log.e(logTag, "Credential request channel consumer has failed with ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Asynchronously triggers  a refresh token request if:
     * - remote is Cells
     * - we have all that we need
     * - no refresh is already running
     */
    override fun requestRefreshToken(stateID: StateID) {
        serviceScope.launch {
            requestRefreshChannel.send(stateID)
        }
    }

    suspend fun getToken(stateID: StateID): Token? = withContext(ioDispatcher) {
        tokenStore.get(stateID.id)
    }

    private suspend fun safelyRefreshToken(stateID: StateID) = withContext(ioDispatcher) {
        synchronized(lock) {
            val token: Token = tokenStore.get(stateID.id) ?: run {
                Log.e(logTag, "Cannot refresh, no token for $stateID")
                return@withContext
            }

            if (token.refreshingSinceTs > 0) {
                // TODO handle a timeout
                Log.e(
                    logTag,
                    "Token for $stateID is already refreshing ignoring<> has just been refreshed, ignoring"
                )
                return@withContext
            }

            if (token.expirationTime > currentTimestamp() + token.expiresIn / 2) {
                // It has been refreshed recently, ignoring
                Log.e(logTag, "Token for $stateID has just been refreshed, ignoring")
                return@withContext
            }

            // Sanity checks
            if (!networkService.isConnected()) {
                Log.e(logTag, "Cannot refresh token $stateID with no access to the remote server")
                return@withContext
            }

            val session = sessionViewDao.getSession(stateID.accountId) ?: run {
                Log.e(logTag, "\"Cannot refresh, unknown session: $stateID\"")
                return@withContext
            }

            // First ping the server: we can use the refresh token only once.
            val serverURL = ServerURLImpl.fromAddress(session.url, session.skipVerify())
            try {
                serverURL.ping()
            } catch (e: Exception) {
                Log.e(
                    logTag,
                    "Could not ping remote at $serverURL, aborting refresh process. ${e.message}"
                )
                return@withContext
            }

            // Insure we have a transport already defined in the store
            val transport = transportStore.get(stateID.accountId)
            if (transport == null) {
                Log.e(logTag, "Cannot refresh, no transport defined for $stateID")
                return@withContext
            } else if (transport !is CellsTransport) {
                Log.e(logTag, "Cannot refresh, transport for $stateID is not for Cells")
                return@withContext
            }

            // Now we start the real refresh token process
            token.refreshingSinceTs = currentTimestamp()
            put(stateID.id, token)

            launch {
                doRefresh(stateID, token, transport)
            }
        }
    }

    private fun doRefresh(
        stateID: StateID,
        token: Token,
        transport: CellsTransport
    ) {
        try {
            Log.e(logTag, "Launching effective refresh token process for $stateID")

            if (Str.empty(token.refreshToken)) {
                Log.e(logTag, "No refresh token available for $stateID, cannot refresh")
                throw SDKException(
                    ErrorCodes.refresh_token_expired,
                    "No refresh token available for $stateID, cannot refresh"
                )
            }

            Log.d(logTag, "### About to wait for refreshed token")
            val newToken = transport.getRefreshedOAuthToken(token.refreshToken)
                ?: throw SDKException(
                    ErrorCodes.cannot_refresh_token,
                    "Refresh token process returned null for $stateID"
                )

            synchronized(lock) {
                // Token has been refreshed
                // Store and return
                put(stateID.id, newToken)
            }
        } catch (re: RemoteIOException) {
            Log.e(logTag, "Could not refresh for $stateID. Still keeping old credentials")
            Log.e(logTag, "  Cause: remoteIOException: ${re.message}")
        } catch (se: Exception) {

            if (se is SDKException && (se.code == ErrorCodes.refresh_token_expired || se.code == ErrorCodes.refresh_token_not_valid)) {
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
            return
        }
    }

}
