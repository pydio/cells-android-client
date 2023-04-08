package com.pydio.android.cells.services

import android.util.Log
import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
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
import com.pydio.android.cells.utils.logException
import com.pydio.cells.api.Client
import com.pydio.cells.api.Credentials
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * AccountService is the single source of truth for accounts, sessions and auth in the app.
 * It takes care of both local caching of session info and authentication against the remote
 * servers.
 */
class AccountService(
    private val networkService: NetworkService,
    accountDB: AccountDB,
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

    fun getClient(stateID: StateID): Client {
        return sessionFactory.getUnlockedClient(stateID.account())
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

    // Expose LiveData for the ViewModels

    fun getLiveSessions() = sessionViewDao.getLiveSessions()

//    @Deprecated("Rather use method with the StateID")
//     fun getLiveSession(accountId: String): LiveData<RSessionView?> =
//        sessionViewDao.getLiveSession(accountId)

    fun getLiveSession(accountID: StateID): LiveData<RSessionView?> =
        sessionViewDao.getLiveSession(accountID.id)

    fun getLiveWorkspaces(accountId: String): LiveData<List<RWorkspace>> =
        workspaceDao.getLiveWorkspaces(accountId)

    fun getLiveWorkspace(stateID: StateID): LiveData<RWorkspace> =
        workspaceDao.getLiveWorkspace(stateID.id)

    fun getLiveWsByType(type: String, accountID: String)
            : LiveData<List<RWorkspace>> {
        return if (type == SdkNames.WS_TYPE_CELL) {
            workspaceDao.getLiveCells(accountID)
        } else {
            workspaceDao.getLiveNotCells(accountID)
        }
    }

    val liveActiveSessionView: LiveData<RSessionView?> =
        sessionViewDao.getLiveActiveSession(AppNames.LIFECYCLE_STATE_FOREGROUND)

    val liveSessionViews: LiveData<List<RSessionView>> = sessionViewDao.getLiveSessions()

    // Direct communication with the backend

    suspend fun isLegacy(stateId: StateID): Boolean = withContext(Dispatchers.IO) {
        return@withContext accountDao.getAccount(stateId.accountId)?.isLegacy ?: false
    }

    suspend fun isRemoteCells(stateId: StateID): Boolean = withContext(Dispatchers.IO) {
        return@withContext !isLegacy(stateId)
    }

    suspend fun getActiveSession(): RSessionView? = withContext(Dispatchers.IO) {
        return@withContext sessionViewDao.getActiveSession(AppNames.LIFECYCLE_STATE_FOREGROUND)
    }

    suspend fun getSession(stateID: StateID): RSessionView? = withContext(Dispatchers.IO) {
        return@withContext sessionViewDao.getSession(stateID.accountId)
    }

    @Throws(SDKException::class)
    suspend fun signUp(serverURL: ServerURL, credentials: Credentials): StateID {
        sessionFactory.registerAccountCredentials(serverURL, credentials)
        val server = sessionFactory.getServer(serverURL.id)
            ?: throw SDKException("could not sign up: unknown server with id ${serverURL.id}")
        // At this point we assume we have been connected or an error has already been thrown
        return registerAccount(credentials.username, server, AppNames.AUTH_STATUS_CONNECTED)
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

    suspend fun getWorkspace(stateID: StateID): RWorkspace? = withContext(Dispatchers.IO) {
        workspaceDao.getWorkspace(stateID.id)
    }

    suspend fun listSessionViews(includeLegacy: Boolean): List<RSessionView> =
        withContext(Dispatchers.IO) {
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
    suspend fun checkRegisteredAccounts(): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            try {
                var changes = 0
                val accounts = accountDao.getAccounts()
                for (account in accounts) {
                    if (account.authStatus != AppNames.AUTH_STATUS_CONNECTED) {
                        continue
                    }
                    if (networkService.isConnected()) {
                        try {
                            // TODO rather use an API health check and implement finer status check (unauthorized, expired...)
                            // sessionFactory.getUnlockedClient(account.accountID)
                            val currClient = sessionFactory.getUnlockedClient(account.account())
                            if (!currClient.stillAuthenticated()) {
                                Log.e(logTag, "${account.accountID} is not connected anymore")
                                account.authStatus = AppNames.AUTH_STATUS_NO_CREDS
                                doUpdateAccount(account)
                                val updatedAccount = accountDao.getAccount(account.accountID)
                                Log.e(logTag, "After update, status: ${updatedAccount?.authStatus}")
                                changes++
                            }
                        } catch (e: SDKException) {
                            Log.e(logTag, "${account.accountID} is not connected: err #${e.code}")
                            account.authStatus = AppNames.AUTH_STATUS_NO_CREDS
                            doUpdateAccount(account)
                            val updatedAccount = accountDao.getAccount(account.accountID)
                            Log.e(logTag, "After update, status: ${updatedAccount?.authStatus}")
//
//                            Log.e(logTag, "Got an error #${e.code} for ${account.accountID}")
//                            e.printStackTrace()
                            changes++
                        }
                    } else {
                        Log.w(
                            logTag, "No network connection, " +
                                    "cannot check auth status for ${account.account()}"
                        )
                    }
                }
                return@withContext Pair(changes, null)
            } catch (e: SDKException) {
                val msg = "could not refresh account list"
                return@withContext Pair(0, msg)
            }
        }

    private suspend fun doUpdateAccount(account: RAccount) = withContext(Dispatchers.IO) {

        if (account.authStatus != AppNames.AUTH_STATUS_CONNECTED) {
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
        fileService.prepareTree(StateID.fromId(account.accountID))
    }

    suspend fun forgetAccount(accountID: StateID): String? = withContext(Dispatchers.IO) {
        //val stateId = StateID.fromId(accountId)
        Log.i(logTag, "### About to forget $accountID")
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

    suspend fun logoutAccount(accountID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            accountDao.getAccount(accountID.id)?.let {
                Log.i(logTag, "About to logout $accountID")
                Log.i(logTag, "Calling stack:")
                Thread.dumpStack()
                // There is also a token that is generated for P8:
                // In case of legacy server, we have to discard a row in **both** tables

                authService.forgetCredentials(accountID, it.isLegacy)
                it.authStatus = AppNames.AUTH_STATUS_NO_CREDS
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
//    @Deprecated("Rather use method with the StateID")
//     suspend fun openSession(accountId: String) {
//        return withContext(Dispatchers.IO) {
//            openSession(StateID.fromId(accountId))
//        }
//    }

    suspend fun openSession(accountID: StateID): RSessionView? {
        return withContext(Dispatchers.IO) {

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
        withContext(Dispatchers.IO) {
            val isConnected = networkService.isConnected()
            val accountID = stateID.account()
            accountDao.getAccount(accountID.id)?.let {
                return@withContext isConnected && it.authStatus == AppNames.AUTH_STATUS_CONNECTED
            }
            return@withContext false
        }

    suspend fun refreshWorkspaceList(accountID: StateID): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            try {
                val client: Client = getClient(accountID)
                val wsDiff = WorkspaceDiff(accountID, client)
                val changeNb = wsDiff.compareWithRemote()
                return@withContext changeNb to null
            } catch (e: SDKException) {
                val msg = "could not get workspace list for $accountID"
                Log.e(logTag, msg)
                e.printStackTrace()
                notifyError(accountID, e.code)
                return@withContext 0 to msg
            }
        }

    private fun isNetworkDownError(code: Int): Boolean {
        return when (code) {
            ErrorCodes.unreachable_host,
            ErrorCodes.no_internet,
            ErrorCodes.con_failed,
            ErrorCodes.con_closed,
            ErrorCodes.con_read_failed,
            ErrorCodes.con_write_failed -> true
            else -> false
        }
    }

    suspend fun notifyError(stateID: StateID, code: Int) = withContext(Dispatchers.IO) {
        Log.e(logTag, "#### Notifying error #$code for $stateID")

        try {
            accountDao.getAccount(stateID.accountId)?.let { currAccount ->

                val msg = "Received error $code for $stateID, old status: ${currAccount.authStatus}"
                Log.i(logTag, msg)

                // First handle network issue
                if (isNetworkDownError(code)) {
//                    Log.e(logTag, "##### Unreachable host")
//                    val networkService: NetworkService = get()
//                    if (networkService.networkInfo()?.isOffline() != true) {
//                        networkService.updateStatus(AppNames.NETWORK_STATUS_NO_INTERNET, code)
//                    }
                    return@withContext
                }

                if (currAccount.isLegacy) {
                    Log.e(logTag, "Got an error but token is refreshing, simply ignoring")
                    return@withContext
                } else {
                    val transport = getTransport(stateID, false)
                    if (transport != null && transport is CellsTransport) {
                        transport.token?.let {
                            if (it.refreshingSinceTs > 1000) {
                                Log.e(
                                    logTag,
                                    "Got an error but token is refreshing, simply ignoring"
                                )
                                return@withContext
                            } else {

                                Log.e(logTag, "##### unexpected error #$code for $stateID")


//                                // Handle Auth Issue
//                                when (code) {
//                                    HttpURLConnection.HTTP_UNAUTHORIZED,
//                                    ErrorCodes.authentication_required -> {
//                                        if (currAccount.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
//                                            currAccount.authStatus =
//                                                AppNames.AUTH_STATUS_UNAUTHORIZED
//                                            doUpdateAccount(currAccount)
//                                        }
//                                        return@withContext
//                                    }
//                                    ErrorCodes.no_token_available,
//                                    ErrorCodes.refresh_token_expired -> {
//                                        if (currAccount.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
//                                            currAccount.authStatus = AppNames.AUTH_STATUS_NO_CREDS
//                                            doUpdateAccount(currAccount)
//                                        }
//                                        return@withContext
//                                    }
//                                    else -> {
//                                        Log.e(logTag, "##### unexpected error $code")
//                                    }
//                                }
                            }
                        } ?: run {
                            Log.e(logTag, "No token found for $stateID")
                            tmpLoop@ for (i in 1..10) {
                                delay(1000)
                                val newTok = transport.token
                                if (newTok == null) {
                                    Log.e(logTag, "Still no token for $stateID")
                                } else {
                                    Log.e(logTag, "Now we have a token for $stateID")
                                    Log.e(logTag, "   - Expiration time: ${newTok.expirationTime}")
                                    Log.e(
                                        logTag,
                                        "   - Refreshing since: ${newTok.refreshingSinceTs}"
                                    )

                                    break@tmpLoop
                                }
                            }
                        }
                    } else {
                        Log.e(logTag, "Notifying error for $stateID without transport")
                        return@withContext
                    }
                }
            }
        } catch (e: Exception) {
            val msg = "Could not update account for $stateID after error $code"
            logException(logTag, msg, e)
        }
        return@withContext
    }
}
