package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.db.accounts.RAccount
import com.pydio.android.cells.db.accounts.RSession
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.db.accounts.SessionViewDao
import com.pydio.android.cells.db.accounts.WorkspaceDao
import com.pydio.android.cells.transfer.WorkspaceDiff
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.logException
import com.pydio.cells.api.Client
import com.pydio.cells.api.Credentials
import com.pydio.cells.api.HttpStatus
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * AccountService is the single source of truth for accounts, sessions and auth in the app.
 * It takes care of both local caching of session info and authentication against the remote
 * servers.
 */
class AccountService(
    coroutineService: CoroutineService,
    accountDB: AccountDB,
    private val networkService: NetworkService,
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
    private val treeNodeRepository: TreeNodeRepository,
    private val fileService: FileService,
) {

    private val logTag = "AccountService"

    private val accountDao: AccountDao = accountDB.accountDao()
    private val sessionDao: SessionDao = accountDB.sessionDao()
    private val sessionViewDao: SessionViewDao = accountDB.sessionViewDao()
    private val workspaceDao: WorkspaceDao = accountDB.workspaceDao()

    private val serviceScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    suspend fun getClient(stateID: StateID): Client {
        try {
            return sessionFactory.getUnlockedClient(stateID.account())
        } catch (se: SDKException) {
            if (se.code == HttpStatus.BAD_GATEWAY.value
                || se.code == HttpStatus.SERVICE_UNAVAILABLE.value
                || se.code == HttpStatus.GATEWAY_TIMEOUT.value
            ) {
                sessionDao.getSession(stateID.accountId)?.let {
                    if (it.isReachable) {
                        it.isReachable = false
                        sessionDao.update(it)
                    }
                }
            }
            throw se
        }
    }

    fun getTransport(stateID: StateID, createIfNeeded: Boolean = false): Transport? {
        return sessionFactory.getTransport(stateID) ?: run {
            var transport: Transport? = null
            if (createIfNeeded) {
                sessionViewDao.getSession(stateID.accountId)?.let {
                    val serverURL = ServerURLImpl.fromAddress(it.url, it.skipVerify())
                    transport = sessionFactory.restoreAccount(serverURL, it.username)
                } ?: run {
                    Log.e(logTag, "No session found for $stateID, cannot get transport")
                    throw SDKException("No session found for $stateID, cannot get transport")
                }
            }
            transport
        }
    }

    // Expose Flows for the ViewModels

    val activeSessionView: Flow<RSessionView?> =
        sessionViewDao.getActiveSessionFlow(AppNames.LIFECYCLE_STATE_FOREGROUND)

    fun getLiveSession(accountID: StateID): Flow<RSessionView?> =
        sessionViewDao.getSessionFlow(accountID.id)

    fun getLiveSessions() = sessionViewDao.getLiveSessions()

    fun getWsByTypeFlow(type: String, accountID: String)
            : Flow<List<RWorkspace>> {
        return if (type == SdkNames.WS_TYPE_CELL) {
            workspaceDao.getCellsFlow(accountID)
        } else {
            workspaceDao.getNotCellsFlow(accountID)
        }
    }

    // Direct communication with the backend

    suspend fun isLegacy(stateId: StateID): Boolean = withContext(ioDispatcher) {
        return@withContext accountDao.getAccount(stateId.accountId)?.isLegacy ?: false
    }

    suspend fun getActiveSession(): RSessionView? = withContext(ioDispatcher) {
        return@withContext sessionViewDao.getActiveSession(AppNames.LIFECYCLE_STATE_FOREGROUND)
    }

    suspend fun getSession(stateID: StateID): RSessionView? = withContext(ioDispatcher) {
        return@withContext sessionViewDao.getSession(stateID.accountId)
    }

    @Throws(SDKException::class)
    suspend fun signUp(serverURL: ServerURL, credentials: Credentials): StateID {
        sessionFactory.registerAccountCredentials(serverURL, credentials)
        val server = sessionFactory.getServer(serverURL.id)
            ?: throw SDKException("could not sign up: unknown server with id ${serverURL.id}")
        // At this point we assume we have been connected or an error has already been thrown
        return registerAccount(credentials.username, server, LoginStatus.Connected.id)
    }

    suspend fun registerAccount(
        username: String,
        server: Server,
        authStatus: String
    ): StateID {

        val account = RAccount.toRAccount(username, server)
        account.authStatus = authStatus

        val state = StateID(username, server.serverURL.id)
        val existingAccount = accountDao.getAccount(state.accountId)

        if (existingAccount == null) { // creation
            accountDao.insert(account)
            safelyCreateSession(account)
        } else { // update
            doUpdateAccount(account)
        }

        return state
    }

    suspend fun getWorkspace(stateID: StateID): RWorkspace? = withContext(ioDispatcher) {
        workspaceDao.getWorkspace(stateID.id)
    }

    suspend fun listSessionViews(includeLegacy: Boolean): List<RSessionView> =
        withContext(ioDispatcher) {
            return@withContext if (includeLegacy) {
                sessionViewDao.getSessions()
            } else {
                sessionViewDao.getCellsSessions()
            }
        }

    /**
     * Performs a check on all accounts that are listed as connected
     * to insure we are still correctly logged in.
     */
    suspend fun checkRegisteredAccounts(): Pair<Int, String?> = withContext(ioDispatcher) {
        try {
            var changes = 0
            val accounts = accountDao.getAccounts()
            accountLoop@ for (rAccount in accounts) {
                if (rAccount.authStatus != LoginStatus.Connected.id) {
                    continue@accountLoop
                }
                if (networkService.isConnected()) {
                    if (checkOneAccount(rAccount)) {
                        changes++
                    }
                } else {
                    Log.w(
                        logTag, "No network connection, " +
                                "cannot check auth status for ${rAccount.accountID()}"
                    )
                }
            }
            return@withContext Pair(changes, null)
        } catch (e: SDKException) {
            val msg = "could not refresh account list"
            return@withContext Pair(0, msg)
        }
    }

    /** Return true if something has changed */
    private suspend fun checkOneAccount(rAccount: RAccount): Boolean {
        val currID = rAccount.accountID()
        try {
            val currClient = sessionFactory.getUnlockedClient(currID)
            if (!currClient.stillAuthenticated()) {
                if (!currClient.isLegacy) {

                    // Finer check for cells, this also launch a refresh token request under the hood
                    val transport = getTransport(rAccount.accountID(), true) as CellsTransport

                    var currToken = transport.token
                    if (currToken == null) {
                        Log.e(logTag, "No token found for $currID, skipping")
                        return false
                    }
                    val timeout = currentTimestamp() + 30
                    while (currentTimestamp() < timeout && (currToken?.isExpired == true)) {
                        delay(2000)
                        currToken = transport.token ?: run {
                            Log.e(logTag, "Token for $currID has disappeared")
                            return true
                        }
                    }

                    if (currToken.isExpired) {
                        Log.e(logTag, "Timeout while waiting for refreshed token for $currID")
                        Log.e(logTag, "   We should consider updating the corresponding session")
                        return true
                    }
                } else { // P8
                    Log.e(logTag, "${rAccount.accountId} is not connected anymore, updating DB")
                    logoutAccount(rAccount.accountID())
                    return true
                }
            } else {
                // We check if the meta of the remote server have changed
                var hasChanged = false
                sessionFactory.getTransport(currID)?.server?.let { server ->
                    //  This throws an exception if we cannot each the server
                    server.refresh(true)

                    // Also insure the server is set has reachable
                    sessionDao.getSession(rAccount.accountId)?.let {
                        if (!it.isReachable) {
                            it.isReachable = true
                            sessionDao.update(it)
                        }
                    }

                    if (Str.notEmpty(server.label) && server.label != rAccount.serverLabel()) {
                        rAccount.setLabel(server.label)
                        hasChanged = true
                    }
                    if (Str.notEmpty(server.welcomeMessage) && server.welcomeMessage != rAccount.welcomeMessage()) {
                        rAccount.setWelcomeMessage(server.welcomeMessage)
                        hasChanged = true
                    }
                    if (Str.notEmpty(server.customPrimaryColor) && server.customPrimaryColor != rAccount.getCustomColor()) {
                        rAccount.setCustomColor(server.customPrimaryColor)
                        hasChanged = true
                    }
//                    if (Str.notEmpty(server.iconURL)) {
//                        Log.w(logTag, "Got an Icon URL: ${server.iconURL} that we **IGNORE**")
//                    }
                }
                if (hasChanged) {
                    doUpdateAccount(rAccount)
                    return true
                }

            }
        } catch (e: SDKException) {
            notifyError(currID, "Unexpected error while checking account", e)
            return true
        }
        return false
    }

    private suspend fun doUpdateAccount(account: RAccount) = withContext(ioDispatcher) {
        if (account.authStatus != LoginStatus.Connected.id) {
            Log.e(logTag, "## About to update account with status ${account.authStatus}")
            Thread.dumpStack()
        }
        accountDao.update(account)
    }

    private suspend fun safelyCreateSession(account: RAccount) {
        // We only update the dir and db names at account creation
        // TODO add tests.
        var session = RSession.newInstance(account, 0)
        val sessionWithSameName = sessionDao.getWithDirName(session.dirName)
        if (sessionWithSameName.isNotEmpty()) {
            session = RSession.newInstance(account, sessionWithSameName.size)
        }
        sessionDao.insert(session)
        treeNodeRepository.refreshSessionCache()
        fileService.prepareTree(StateID.fromId(account.accountId))
    }

    suspend fun forgetAccount(accountID: StateID): String? = withContext(ioDispatcher) {
        Log.i(logTag, "... About to forget $accountID")
        try {
            val oldAccount = accountDao.getAccount(accountID.id)
                ?: return@withContext null // nothing to forget

            // Downloaded files
            fileService.cleanAllLocalFiles(accountID)
            // Credentials
            authService.forgetCredentials(accountID, oldAccount.isLegacy)
            // Remove rows in the account tables
            sessionDao.forgetSession(accountID.id)
            workspaceDao.forgetAccount(accountID.id)
            accountDao.forgetAccount(accountID.id)
            treeNodeRepository.closeNodeDb(accountID.accountId)

            // Update local caches
            treeNodeRepository.refreshSessionCache()

            Log.i(logTag, "### $accountID has been forgotten")
            return@withContext null
        } catch (e: Exception) {
            val msg = "Could not delete account $accountID"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    suspend fun logoutAccount(accountID: StateID): String? = withContext(ioDispatcher) {
        Log.e(logTag, "In logout account for $accountID")
        try {
            accountDao.getAccount(accountID.id)?.let {
                Log.i(logTag, "About to logout $accountID")
                // There is also a token that is generated for P8:
                // In case of legacy server, we have to discard a row in **both** tables
                authService.forgetCredentials(accountID, it.isLegacy)
                it.authStatus = LoginStatus.NoCreds.id
                doUpdateAccount(it)
                return@withContext null
            }
        } catch (e: Exception) {
            val msg = "Could not delete credentials for $accountID}"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    /**
     * Sets the lifecycle_state of a given session to "foreground".
     * WARNING: no check is done on the passed accountID.
     */
    suspend fun openSession(accountID: StateID): RSessionView? {
        return withContext(ioDispatcher) {

            // Check if a session with this ISD exists
            val newSession = sessionDao.getSession(accountID.id)
                ?: run {
                    Log.e(logTag, "No session found for $accountID")
                    return@withContext null
                }

            // Put other opened sessions in background
            val tmpSessions = sessionDao.listAllForegroundSessions()
            for (currSession in tmpSessions) {
                currSession.lifecycleState = AppNames.LIFECYCLE_STATE_BACKGROUND
                sessionDao.update(currSession)
            }

            // Update session state and return corresponding session view
            newSession.lifecycleState = AppNames.LIFECYCLE_STATE_FOREGROUND
            sessionDao.update(newSession)
            return@withContext getSession(accountID)
        }
    }

    suspend fun isClientConnected(stateID: StateID): Boolean =
        withContext(ioDispatcher) {
            val isConnected = networkService.isConnected()
            val accountID = stateID.account()
            accountDao.getAccount(accountID.id)?.let {
                return@withContext isConnected && it.authStatus == LoginStatus.Connected.id
            }
            return@withContext false
        }

    suspend fun refreshWorkspaceList(accountID: StateID): Pair<Int, String?> =
        withContext(ioDispatcher) {
            try {
                val client: Client = getClient(accountID)
                val wsDiff = WorkspaceDiff(accountID, client)
                val changeNb = wsDiff.compareWithRemote()
                return@withContext changeNb to null
            } catch (e: SDKException) {
                val msg = "Could not get workspace list for $accountID"
                Log.e(logTag, msg)
                e.printStackTrace()
                notifyError(accountID, msg, e)
                return@withContext 0 to msg
            }
        }

    suspend fun notifyError(
        stateID: StateID, msg: String, se: SDKException
    ) = withContext(ioDispatcher) {
        Log.i(logTag, "Notifying error for $stateID: #${se.code} - ${se.message}")
        try {
            accountDao.getAccount(stateID.accountId)?.let { currAccount ->
                val msg2 = "Received error ${se.code} for $stateID, message: $msg, " +
                        "Old status: ${currAccount.authStatus}"
                Log.w(logTag, msg2)
                // First handle network issue
                if (se.isNetworkError) {
                    sessionDao.getSession(stateID.accountId)?.let {
                        if (it.isReachable) { // Update reachable flag ASAP
                            it.isReachable = false
                            sessionDao.update(it)
                        }
                    }
                    return@withContext
                }

                if (currAccount.isLegacy) {
                    Log.w(logTag, "Error while connecting to remote P8 server, ignoring")
                    return@withContext
                }

                if (se.isAuthorizationError) {
                    serviceScope.launch {
                        // We use another coroutine to give time to retry...
                        handleAuthError(stateID)
                    }
                } else {
                    Log.e(
                        logTag, "Unexpected error #${se.code}: ${se.message}," +
                                " simply ignoring for the time being"
                    )
                }
                return@withContext
            }
        } catch (e: Exception) {
            val msg2 = "Could not update account for $stateID after error #${se.code}: $msg"
            logException(logTag, msg2, e)
        }
        return@withContext
    }

    private suspend fun handleAuthError(stateID: StateID) {
        serviceScope.launch {

            val transport = try {
                getTransport(stateID, true)
            } catch (e: Exception) {
                Log.e(logTag, "Could not retrieve transport, cause: ${e.message}")
                Thread.dumpStack()
                return@launch
            }
            if (transport == null || transport !is CellsTransport) {
                // We should never land here an exception must have already been thrown
                Log.e(logTag, "No transport, ignoring error")
                Log.e(logTag, "  but we should not be there, printing stack:")
                Thread.dumpStack()
                return@launch
            }

            Log.e(logTag, "In handle auth error, token: ${transport.token}")

            transport.token?.let {

                if (!it.isExpired) {
                    // Handle corner case when we have been kicked off the server:
                    // We perform a supplementary check to insure we still can connect to the server
                    try {
                        Log.d(logTag, "## Trying to download boot configuration for $stateID")
                        transport.tryDownloadingBootConf()
                    } catch (e: Exception) {
                        // Cannot get boot conf, so token is not valid anymore
                        if (e is FileNotFoundException) {
                            var i = 1
                            loop@ while (i < 4) {
                                delay(1000)
                                i++
                                try {
                                    Log.d(logTag, "#$i: trying to get boot conf for $stateID")
                                    transport.tryDownloadingBootConf()
                                    break@loop
                                } catch (e: FileNotFoundException) {
                                    Log.d(logTag, "#$i exception: ${e.message}")
                                    if (i == 4) {
                                        Log.d(
                                            logTag,
                                            "Timeout reached, about to logout from ${stateID.account()}"
                                        )
                                        // finally logout
                                        logoutAccount(stateID.account())
                                        return@launch
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        logTag,
                                        "try $i: unexpected error while getting boot config"
                                    )
                                    e.printStackTrace()
                                    return@launch
                                }
                            }
                        } else {
                            return@launch
                        }
                    }
                }

                if (it.refreshingSinceTs > 1000) {
                    Log.e(logTag, "Got an error but token is refreshing, ignoring")
                } else { // We rely on the transport to update our local repo if necessary
                    transport.requestTokenRefresh()
                }
                return@launch
            }
        }
    }


//    private class DummyHandler(stateID: StateID) : DefaultHandler() {
//        private val logTag = "DummyHandler"
//
//        var currPath = ""
//
//        init {
//            Log.e(logTag, "New Dummy handler for $stateID")
//        }
//
//        @Throws(SAXException::class)
//        override fun startElement(
//            uri: String?,
//            localName: String?,
//            qName: String?,
//            attributes: Attributes?
//        ) {
//            currPath += "/$localName"
//            if (!currPath.startsWith("/pydio_registry/actions/act"))
//                Log.e(logTag, "$currPath $uri")
//        }
//
//        @Throws(SAXException::class)
//        override fun endElement(uri: String?, localName: String?, qName: String?) {
//            currPath.removeSuffix("/$localName")
//            // Log.e(logTag, "END - $localName $uri")
//        }
//    }
}
