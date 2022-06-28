package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.cells.api.Client
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.Server
import com.pydio.cells.api.Store
import com.pydio.cells.api.Transport
import com.pydio.cells.client.CellsClient
import com.pydio.cells.client.ClientFactory
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.CredentialService
import com.pydio.cells.utils.Str

/**
 * The android specific session factory wraps the Java SDK Server and Client Factories,
 * to provide a Android specific S3 client for file transfer with cells.
 * It also holds server and transport memory stores that are used by the SDK to manage clients.
 */
class SessionFactory(
    private val networkService: NetworkService,
    credentialService: CredentialService,
    serverStore: Store<Server>,
    private val transportStore: Store<Transport>,
    accountDB: AccountDB
) : ClientFactory(credentialService, serverStore, transportStore) {

    private val logTag = SessionFactory::class.java.simpleName
//    private var sessionFactoryJob = Job()
//    private val sessionFactoryScope = CoroutineScope(Dispatchers.IO + sessionFactoryJob)
//

    private val sessionViewDao = accountDB.sessionViewDao()
    private val accountDao = accountDB.accountDao()

//    private var ready = false

//    init {
//        sessionFactoryScope.launch(Dispatchers.IO) {
//            val sessions = sessionViewDao.getSessions()
//            // val accounts = accountService.accountDB.accountDao().getAccounts()
//            Log.i(logTag, "... Initialise SessionFactory")
//            for (rLiveSession in sessions) {
//                // TODO skip sessions when we know they are not usable?
//                Log.i(logTag, "... Preparing transport for ${rLiveSession.getStateID()}")
//                try {
//                    prepareTransport(rLiveSession)
//                } catch (e: SDKException) {
//                    // TODO update live session depending on the error
//                    Log.e(
//                        logTag,
//                        "Cannot restore session for " + rLiveSession.accountID + ": " + e.message
//                    )
//                }
//            }
//            Log.i(logTag, "... Session factory initialised")
//            ready = true
//        }
//    }

    override fun getServer(id: String): Server? {
        val stateID = StateID.fromId(id)
        super.getServer(id)?.let { return it }

        // If not yet defined, try to register it
        val sessionView = sessionViewDao.getSession(stateID.accountId)
        if (sessionView != null) {
            var currTransport = transportStore.get(stateID.accountId)
            if (currTransport == null) {
                currTransport = prepareTransport(sessionView)
            }
            return currTransport?.server
        }

        if (Str.empty(stateID.username)) {
            // We check if an account with same URL is registered and get the server
            val accounts = accountDao.getAccountByUrl(stateID.serverUrl)
            if (accounts.isNotEmpty()) {
                val serverURL = ServerURLImpl.fromAddress(accounts[0].url, accounts[0].skipVerify())
                return registerServer(serverURL)
            }
        }

        throw SDKException(ErrorCodes.not_found, "cannot retrieve server for $stateID")
    }

    @Throws(SDKException::class)
    fun getUnlockedClient(accountID: String): Client {

        if (!networkService.isConnected())
            throw SDKException(ErrorCodes.no_internet, "No internet connection is available")


//        // TODO An idea might be to leave a hook here to enable network status refresh
//        if (!forceCall && networkService.networkInfo()?.isOnline() != true) {
//            Log.d(logTag, "... Refreshing network status")
//            try {
//                var serverURL = transportStore.get(accountID)?.server?.serverURL
//                if (serverURL == null) {
//                    for (currentSession in sessionViewDao.getSessions()) {
//                        if (currentSession.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
//                            serverURL = ServerURLImpl.fromAddress(
//                                currentSession.url,
//                                currentSession.skipVerify()
//                            )
//                            break
//                        }
//                    }
//                }
//                if (serverURL != null) {
//                    serverURL.ping()
//                    // No exception, network is back
//                    networkService.updateStatus(AppNames.NETWORK_STATUS_OK, 200)
//                    Log.d(logTag, "    ---> back online")
//                } else {
//                    throw SDKException(
//                        ErrorCodes.no_internet,
//                        "Could not check if internet is back"
//                    )
//                }
//            } catch (e: Exception) {
//                // Expected, we cannot ping server
////                Log.d(logTag, "   Error while pinging server: " + e.message)
////                e.printStackTrace()
//                val code = if (e is SDKException) e.code else ErrorCodes.no_internet
//                if (networkService.networkInfo()?.isOffline() != true) {
//                    networkService.updateStatus(AppNames.NETWORK_STATUS_NO_INTERNET, code)
//                }
//                Log.e(logTag, "    ---> still offline")
//                throw SDKException(ErrorCodes.no_internet, "No internet connection is available")
//            }
//        }

//        if (!hasAtLeastMeteredNetwork(CellsApp.instance.applicationContext)) {
//            throw SDKException(ErrorCodes.no_internet, "No internet connection is available")
//        }
        return internalGetClient(accountID)
    }

    fun getUnlockedUnMeteredClient(accountID: String): Client {

        if (networkService.isConnected() && networkService.isMetered())
            throw SDKException(
                ErrorCodes.no_internet,
                "No un-metered internet connection is available"
            )

//        if (!hasUnMeteredNetwork(CellsApp.instance.applicationContext)) {
//            throw SDKException(
//                ErrorCodes.no_un_metered_connection,
//                "No un-metered connection available"
//            )
//        }
        return internalGetClient(accountID)
    }

    @Throws(SDKException::class)
    private fun internalGetClient(accountID: String): Client {

        // At this point we are quite sure we have a connection to the internet...
        // Yet we still code defensively afterwards and correctly handle errors
        val sessionView: RSessionView = sessionViewDao.getSession(accountID)
            ?: run {
                throw SDKException(ErrorCodes.not_found, "cannot retrieve client for $accountID")
            }

        if (sessionView.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
            var currTransport = transportStore.get(accountID)
            if (currTransport == null) {
                currTransport = prepareTransport(sessionView)
            }
            return getClient(currTransport)
        } else {

            Log.d(logTag, "... Required session is not connected, listing known sessions:")
            for (currentSession in sessionViewDao.getSessions()) {
                Log.d(logTag, "$currentSession.dbName} / ${currentSession.authStatus}")
            }

            throw SDKException(
                ErrorCodes.authentication_required,
                "cannot unlock session for $accountID, auth status: " + sessionView.authStatus
            )
        }
    }

    private fun prepareTransport(sessionView: RSessionView): Transport {
        try {
            val skipVerify = sessionView.tlsMode == 1
            val serverURL = ServerURLImpl.fromAddress(sessionView.url, skipVerify)
            return restoreAccount(serverURL, sessionView.username)
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

//    private fun registerTransport(accountID: StateID): Transport {
//        try {
//            val skipVerify = sessionView.tlsMode == 1
//            val serverURL = ServerURLImpl.fromAddress(sessionView.url, skipVerify)
//            return restoreAccount(serverURL, sessionView.username)
//        } catch (se: SDKException) {
////            Log.e(logTag, "could not resurrect session: " + se.message)
////            // Handle well known errors and transfer the error to the caller
////            when (se.code) {
////                ErrorCodes.authentication_required -> {
////                    account.authStatus = AppNames.AUTH_STATUS_NO_CREDS
////                    db.accountDao().update(account)
////                }
////                ErrorCodes.token_expired -> {
////                    account.authStatus = AppNames.AUTH_STATUS_EXPIRED
////                    db.accountDao().update(account)
////                }
////            }
//            throw se
//        }
// }


    /**
     * Enables the use of a android specific S3 client by the ancestor classes in the JAVA only SDK
     * */
    override fun getCellsClient(transport: CellsTransport?): CellsClient {
        return CellsClient(transport, S3Client(transport))
    }
}
