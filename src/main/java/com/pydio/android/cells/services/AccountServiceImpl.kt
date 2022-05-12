package com.pydio.android.cells.services

import android.util.Log
import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.db.accounts.LegacyCredentialsDao
import com.pydio.android.cells.db.accounts.RAccount
import com.pydio.android.cells.db.accounts.RSession
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.db.accounts.SessionViewDao
import com.pydio.android.cells.db.accounts.TokenDao
import com.pydio.android.cells.db.accounts.WorkspaceDao
import com.pydio.android.cells.db.accounts.toRAccount
import com.pydio.android.cells.transfer.WorkspaceDiff
import com.pydio.android.cells.utils.hasAtLeastMeteredNetwork
import com.pydio.android.cells.utils.logException
import com.pydio.cells.api.Client
import com.pydio.cells.api.Credentials
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.Server
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.net.HttpURLConnection

/**
 * AccountService is the single source of truth for accounts, sessions and auth in the app.
 * It takes care of both local caching of session info and authentication against the remote
 * servers.
 */
class AccountServiceImpl(
    accountDB: AccountDB,
    private val sessionFactory: SessionFactory,
    private val treeNodeRepository: TreeNodeRepository
) : AccountService, KoinComponent {

    private val logTag = AccountService::class.java.simpleName

    private val accountDao: AccountDao = accountDB.accountDao()
    private val sessionDao: SessionDao = accountDB.sessionDao()
    private val sessionViewDao: SessionViewDao = accountDB.liveSessionDao()
    private val workspaceDao: WorkspaceDao = accountDB.workspaceDao()
    private val tokenDao: TokenDao = accountDB.tokenDao()
    private val legacyCredentialsDao: LegacyCredentialsDao = accountDB.legacyCredentialsDao()

    override suspend fun getSession(stateId: StateID): RSessionView? {
        return sessionViewDao.getSession(stateId.accountId)
    }

    override fun getClient(stateId: StateID): Client {
        return sessionFactory.getUnlockedClient(stateId.accountId)
    }

    override fun getLiveSessions() = sessionViewDao.getLiveSessions()

    override fun getLiveSession(accountID: String): LiveData<RSessionView?> =
        sessionViewDao.getLiveSession(accountID)

    override fun getLiveWorkspaces(accountID: String): LiveData<List<RWorkspace>> =
        workspaceDao.getLiveWorkspaces(accountID)

    override val liveActiveSessionView: LiveData<RSessionView?> =
        sessionViewDao.getLiveActiveSession(AppNames.LIFECYCLE_STATE_FOREGROUND)

    override val liveSessionViews: LiveData<List<RSessionView>> = sessionViewDao.getLiveSessions()

    @Throws(SDKException::class)
    override suspend fun signUp(serverURL: ServerURL, credentials: Credentials): String {
        sessionFactory.registerAccountCredentials(serverURL, credentials)
        val server: Server = sessionFactory.getServer(serverURL.id)
        // At this point we assume we have been connected or an error has already been thrown
        val state = registerAccount(credentials.username, server, AppNames.AUTH_STATUS_CONNECTED)
        return state.id
    }

    override suspend fun registerAccount(
        username: String,
        server: Server,
        authStatus: String
    ): StateID {

        val account = toRAccount(username, server)
        account.authStatus = authStatus

        val state = StateID(username, server.serverURL.id)
        val existingAccount = accountDao.getAccount(state.accountId)

        if (existingAccount == null) { // creation
            accountDao.insert(account)
            safelyCreateSession(account)
        } else { // update
            accountDao.update(account)
        }

        return state
    }

    override fun listSessionViews(includeLegacy: Boolean): List<RSessionView> {
        return if (includeLegacy) {
            sessionViewDao.getSessions()
        } else {
            sessionViewDao.getCellsSessions()
        }
    }

    /**
     * Performs a check on all accounts that are listed as connected
     * to insure we are still correctly logged in.
     */
    override suspend fun checkRegisteredAccounts(): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            try {
                var changes = 0
                val accounts = accountDao.getAccounts()
                for (account in accounts) {
                    try {
                        if (account.authStatus != AppNames.AUTH_STATUS_CONNECTED) {
                            continue;
                        }
                        // TODO rather use an API healthcheck
                        val currClient = sessionFactory.getUnlockedClient(account.accountID)
                        if (!currClient.stillAuthenticated()) {
                            account.authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED
                            accountDao.update(account)
                            changes++
                        }
                    } catch (e: SDKException) {
                        // First handle network issue
                        if (isNetworkDownError(e.code)) {
                            Log.e(logTag, "##### Unreachable host")
                            val networkService: NetworkService = get()
                            if (networkService.networkInfo()?.isOffline() != true) {
                                networkService.updateStatus(
                                    AppNames.NETWORK_STATUS_NO_INTERNET,
                                    e.code
                                )
                            }
                            return@withContext Pair(0, "Network down")
                        }
                        // TODO insure we do not miss anything
                        Log.e(logTag, "Got an error #${e.code} for ${account.accountID}")
                        e.printStackTrace()
                    }
                }
                return@withContext Pair(changes, null)
            } catch (e: SDKException) {
                val msg = "could not refresh account list"
                return@withContext Pair(0, msg)
            }
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

        val fileService: FileService = get()
        fileService.prepareTree(StateID.fromId(account.accountID))
    }

    override suspend fun forgetAccount(accountId: String): String? = withContext(Dispatchers.IO) {
        val stateId = StateID.fromId(accountId)
        Log.d(logTag, "### About to forget $stateId")
        try {
            val oldAccount = accountDao.getAccount(accountId)
                ?: return@withContext null // nothing to forget

            val dirName = treeNodeRepository.sessions[accountId]?.dirName
                ?: throw IllegalStateException("No record found for $stateId")

            // Credentials
            if (oldAccount.isLegacy) {
                legacyCredentialsDao.forgetPassword(accountId)
            } else {
                tokenDao.deleteToken(accountId)
            }

            val fileService: FileService = get()
            fileService.cleanAllLocalFiles(stateId, dirName)

            // Remove rows in the account tables
            sessionDao.forgetSession(accountId)
            workspaceDao.forgetAccount(accountId)
            accountDao.forgetAccount(accountId)

            val treeNodeRepository: TreeNodeRepository = get()
            treeNodeRepository.closeNodeDb(stateId.accountId)

            // Update local caches
            treeNodeRepository.refreshSessionCache()

            Log.i(logTag, "### $stateId has been forgotten")
            return@withContext null
        } catch (e: Exception) {
            val msg = "Could not delete account ${StateID.fromId(accountId)}"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    override suspend fun logoutAccount(accountID: String): String? = withContext(Dispatchers.IO) {
        try {
            accountDao.getAccount(accountID)?.let {
                Log.i(logTag, "About to logout $accountID")
                Log.i(logTag, "Calling stack:")
                Thread.dumpStack()
                // There is also a token that is generated for P8:
                // In case of legacy server, we have to discard a row in **both** tables
                if (it.isLegacy) {
                    legacyCredentialsDao.forgetPassword(accountID)
                }
                tokenDao.deleteToken(accountID)
                it.authStatus = AppNames.AUTH_STATUS_NO_CREDS
                accountDao.update(it)
                return@withContext null
            }
        } catch (e: Exception) {
            val msg = "Could not delete credentials for ${StateID.fromId(accountID)}"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    /**
     * Sets the lifecycle_state of a given session to "foreground".
     * WARNING: no check is done on the passed accountID.
     */
    override suspend fun openSession(accountID: String) {
        return withContext(Dispatchers.IO) {

            // First put other opened sessions in the background
            val tmpSessions = sessionDao.listAllForegroundSessions()
            for (currSession in tmpSessions) {
                currSession.lifecycleState = AppNames.LIFECYCLE_STATE_BACKGROUND
                sessionDao.update(currSession)
            }

            val openSession = sessionDao.getSession(accountID)

            if (openSession == null) {
                // should never happen
                Log.e(logTag, "No session found for $accountID")
//                openSession = fromAccountID(accountID)
//                accountDB.sessionDao().insert(openSession)
            } else {
                openSession.lifecycleState = AppNames.LIFECYCLE_STATE_FOREGROUND
                sessionDao.update(openSession)
            }
        }
    }

    override suspend fun isClientConnected(stateID: String): Boolean = withContext(Dispatchers.IO) {
        val isConnected = hasAtLeastMeteredNetwork(CellsApp.instance.applicationContext)
        val accountID = StateID.fromId(stateID).accountId
        accountDao.getAccount(accountID)?.let {
            return@withContext isConnected && it.authStatus == AppNames.AUTH_STATUS_CONNECTED
        }
        return@withContext false
    }

    override suspend fun refreshWorkspaceList(accountIDStr: String): Pair<Int, String?> =
        withContext(Dispatchers.IO) {
            val result: Pair<Int, String?>

            val accountID = StateID.fromId(accountIDStr)
            try {
                val client: Client = getClient(accountID)
                val wsDiff = WorkspaceDiff(accountID, client)
                val changeNb = wsDiff.compareWithRemote()
                result = Pair(changeNb, null)
            } catch (e: SDKException) {
                val msg = "could not get workspace list for $accountID"
                Log.e(logTag, msg)
                e.printStackTrace()
                notifyError(accountID, e.code)
                return@withContext Pair(0, msg)
            }
            return@withContext result
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

    override suspend fun notifyError(stateID: StateID, code: Int) = withContext(Dispatchers.IO) {
        try {
            accountDao.getAccount(stateID.accountId)?.let { currAccount ->
                val msg = "Received error $code for $stateID, old status: ${currAccount.authStatus}"
                Log.i(logTag, msg)

                // First handle network issue
                if (isNetworkDownError(code)) {
                    Log.e(logTag, "##### Unreachable host")
                    val networkService: NetworkService = get()
                    if (networkService.networkInfo()?.isOffline() != true) {
                        networkService.updateStatus(AppNames.NETWORK_STATUS_NO_INTERNET, code)
                    }
                    return@withContext
                }

                // Handle Auth Issue
                when (code) {
                    HttpURLConnection.HTTP_UNAUTHORIZED,
                    ErrorCodes.authentication_required -> {
                        if (currAccount.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
                            currAccount.authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED
                            accountDao.update(currAccount)
                        }
                        return@withContext
                    }
                    ErrorCodes.no_token_available,
                    ErrorCodes.refresh_token_expired -> {
                        if (currAccount.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
                            currAccount.authStatus = AppNames.AUTH_STATUS_NO_CREDS
                            accountDao.update(currAccount)
                        }
                        return@withContext
                    }
                    else -> {
                        Log.e(logTag, "##### unexpected error $code")
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
