package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.AccountDao
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
import kotlinx.coroutines.delay
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
    private val procDispatcher: CoroutineDispatcher,
    private val networkService: NetworkService,
    private val accountDao: AccountDao,
    private val sessionViewDao: SessionViewDao,
) : CredentialService(tokenStore, passwordStore), KoinComponent {

    private val logTag = "AppCredentialService"

    // Insure we only process one refresh request at a time
    private val requestRefreshChannel = Channel<StateID>()

    // Semaphore for the refresh process.
    private val lock = Any()

    private val credServiceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(ioDispatcher + credServiceJob)

    private val maxFail = 300
    private var failNb: Int = 0

    init {
        serviceScope.launch {
            withContext(procDispatcher) {
                while (failNb < maxFail) {
                    try {
                        val currID = requestRefreshChannel.receive()
                        Log.e(logTag, "Received refresh token request for $currID")
                        safelyRefreshToken(currID)
                        failNb = 0
                    } catch (e: Exception) {
                        Log.e(
                            logTag,
                            "Credential request channel consumer has failed with ${e.message}"
                        )
                        e.printStackTrace()
                        delay(2000)
                        failNb++
                    }
                }

                Log.e(
                    logTag,
                    "### Too many errors while trying to read channel"
                )
                Thread.dumpStack()
                // We failed more than 1000 times in a row
                throw SDKException(
                    ErrorCodes.internal_error,
                    "Too many errors while trying to read channel"
                )
            }
        }
    }

    suspend fun getToken(stateID: StateID): Token? = withContext(ioDispatcher) {
        tokenStore.get(stateID.id)
    }

    /**
     * Asynchronously triggers  a refresh token request if:
     * - remote is Cells
     * - we have all that we need
     * - no refresh is already running
     */
    override fun requestRefreshToken(stateID: StateID) {
        serviceScope.launch {
            Log.d(logTag, "Sending refresh token request for $stateID")
            requestRefreshChannel.send(stateID)
        }
    }

    private suspend fun safelyRefreshToken(stateID: StateID) = withContext(ioDispatcher) {
        synchronized(lock) {
            val token: Token = tokenStore.get(stateID.id) ?: run {
                Log.e(logTag, "Cannot refresh, no token for $stateID")
                return@withContext
            }

            if (token.refreshingSinceTs > 0) {
                // TODO handle a timeout
                Log.e(logTag, "Token for $stateID is already refreshing ignoring")
                return@withContext
            }

            if (token.expirationTime > currentTimestamp() + token.expiresIn / 2) {
                // It has been refreshed recently, ignoring
                Log.d(logTag, "Token for $stateID has just been refreshed, ignoring")
                return@withContext
            }

            // Sanity checks
            if (!networkService.isConnected()) {
                Log.e(
                    logTag,
                    "Cannot refresh token $stateID with no access to the remote server"
                )
                return@withContext
            }

            val session = sessionViewDao.getSession(stateID.accountId) ?: run {
                Log.e(logTag, "Cannot refresh, unknown session: $stateID")
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
            Log.i(logTag, "Launching effective refresh token process for $stateID")

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

            // Token has been refreshed Store and return
            synchronized(lock) {
                put(stateID.id, newToken)
            }
            accountDao.getAccount(stateID.accountId)?.let {
                it.authStatus = AppNames.AUTH_STATUS_CONNECTED
                accountDao.update(it)
            }
            Log.i(logTag, "Refresh token process done for $stateID")
        } catch (re: RemoteIOException) {
            Log.e(logTag, "Could not refresh for $stateID. Still keeping old credentials")
            Log.e(logTag, "  Cause: remoteIOException: ${re.message}")
        } catch (se: Exception) {

            if (se is SDKException && (se.code == ErrorCodes.refresh_token_expired || se.code == ErrorCodes.refresh_token_not_valid)) {
                // Could not refresh, finally deleting credentials to avoid being stuck
                logout(stateID, "#${se.code}: ${se.message}")
                throw se
            }
            Log.e(logTag, "Could not refresh for $stateID, unexpected exception: ${se.message}")
            se.printStackTrace()
            Log.e(logTag, "Keep old credentials and abort.")
            return
        }
    }

    private fun logout(stateID: StateID, cause: String) {
        Log.e(logTag, "########################################")
        Log.e(logTag, "### Refresh token expired for $stateID")
        Log.e(logTag, "  Cause: $cause")
        Log.e(logTag, "  !! Removing stored credentials !! ")
        synchronized(lock) {
            remove(stateID.id)
        }
        accountDao.getAccount(stateID.accountId)?.let {
            it.authStatus = AppNames.AUTH_STATUS_NO_CREDS
            accountDao.update(it)
        }
    }
}
