package com.pydio.android.cells.services

import android.util.Log
import com.pydio.cells.api.*
import com.pydio.cells.client.CellsClient
import com.pydio.cells.client.ClientFactory
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.auth.CredentialService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.LiveSessionDao
import com.pydio.android.cells.db.accounts.RLiveSession
import com.pydio.android.cells.utils.hasAtLeastMeteredNetwork
import com.pydio.android.cells.utils.hasUnMeteredNetwork

/**
 * The android specific session factory wraps the Java SDK Server and Client Factories,
 * to provide a Android specific S3 client for file transfer with cells.
 * It also holds server and transport memory stores that are used by the SDK to manage clients.
 */
class SessionFactory(
    credentialService: CredentialService,
    serverStore: Store<Server>,
    val transportStore: Store<Transport>,
    private val liveSessionDao: LiveSessionDao,
) : ClientFactory(credentialService, serverStore, transportStore) {

    private val logTag = SessionFactory::class.java.simpleName
    private var sessionFactoryJob = Job()
    private val sessionFactoryScope = CoroutineScope(Dispatchers.IO + sessionFactoryJob)

    private var ready = false

    init {
        sessionFactoryScope.launch(Dispatchers.IO) {
            val sessions = liveSessionDao.getSessions()
            // val accounts = accountService.accountDB.accountDao().getAccounts()
            Log.i(logTag, "... Initialise SessionFactory")
            for (rLiveSession in sessions) {
                // TODO skip sessions when we know they are not usable?
                Log.i(logTag, "... Preparing transport for ${rLiveSession.getStateID()}")
                try {
                    prepareTransport(rLiveSession)
                } catch (e: SDKException) {
                    // TODO update live session depending on the error
                    Log.e(
                        logTag,
                        "Cannot restore session for " + rLiveSession.accountID + ": " + e.message
                    )
                }
            }
            Log.i(logTag, "... Session factory initialised")
            ready = true
        }
    }

    @Throws(SDKException::class)
    fun getUnlockedClient(accountID: String): Client {
        if (!hasAtLeastMeteredNetwork(CellsApp.instance.applicationContext)) {
            throw SDKException(ErrorCodes.no_internet, "No internet connection is available")
        }

        return internalGetClient(accountID)
    }

    @Throws(SDKException::class)
    fun getUnlockedUnMeteredClient(accountID: String): Client {
        if (!hasUnMeteredNetwork(CellsApp.instance.applicationContext)) {
            throw SDKException(
                ErrorCodes.no_un_metered_connection,
                "No un-metered connection available"
            )
        }
        return internalGetClient(accountID)
    }

    @Throws(SDKException::class)
    private fun internalGetClient(accountID: String): Client {

        // At this point we are quite sure we have a connection to the internet...
        // Yet we still code defensively afterwards and correctly handle errors
        val session: RLiveSession = liveSessionDao.getSession(accountID)
            ?: run {
                throw SDKException(ErrorCodes.not_found, "cannot retrieve client for $accountID")
            }

        if (session.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
            var currTransport = transportStore.get(accountID)
            if (currTransport == null) {
                currTransport = prepareTransport(session)
            }
            return getClient(currTransport)
        } else {

            Log.d(logTag, "... Required session is not connected, listing known sessions:")
            for (currentSession in liveSessionDao.getSessions()) {
                Log.d(logTag, "$currentSession.dbName} / ${currentSession.authStatus}")
            }

            throw SDKException(
                ErrorCodes.authentication_required,
                "cannot unlock session for $accountID, auth status: " + session.authStatus
            )
        }
    }

    @Throws(SDKException::class)
    private fun prepareTransport(session: RLiveSession): Transport {
        try {
            val skipVerify = session.tlsMode == 1
            val serverURL = ServerURLImpl.fromAddress(session.url, skipVerify)
            return restoreAccount(serverURL, session.username)

        } catch (se: SDKException) {
//            Log.e(logTag, "could not resurrect session: " + se.message)
//            // Handle well known errors and transfer the error to the caller
//            when (se.code) {
//                ErrorCodes.authentication_required -> {
//                    account.authStatus = AppNames.AUTH_STATUS_NO_CREDS
//                    db.accountDao().update(account)
//                }
//                ErrorCodes.token_expired -> {
//                    account.authStatus = AppNames.AUTH_STATUS_EXPIRED
//                    db.accountDao().update(account)
//                }
//            }
            throw se
        }
    }

    /**
     * Enables the use of a android specific S3 client by the ancestor classes in the JAVA only SDK
     * */
    override fun getCellsClient(transport: CellsTransport?): CellsClient {
        return CellsClient(transport, S3Client(transport))
    }
}
