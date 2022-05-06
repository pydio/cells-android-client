package com.pydio.android.cells.legacy.db

import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.legacy.db.model.AccountRecord
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.transport.auth.credentials.LegacyPasswordCredentials
import com.pydio.cells.utils.Log
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import java.io.File

class LegacyMigrationTest : AutoCloseKoinTest() {

    private val logTag = LegacyMigrationTest::class.simpleName

    private val accountService by inject<AccountService>()
    private val sessionFactory by inject<SessionFactory>()
    private val nodeService by inject<NodeService>()

    @Test
    fun migrateAllAccounts() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Insure we have all legacy DBs and init
        val mainDbPath = context.dataDir.absolutePath + MainDB.DB_FILE_PATH
        val mainDbFile = File(mainDbPath)
        val syncDbPath = context.dataDir.absolutePath + SyncDB.DB_FILE_PATH
        val syncDbFile = File(syncDbPath)
        if (!mainDbFile.exists()) {
            Log.i(logTag, "... No Legacy Main DB file found at $mainDbPath")
            return@runTest
        }
        if (!syncDbFile.exists()) {
            Log.i(logTag, "... No Legacy Sync DB file found at $syncDbPath")
            return@runTest
        }
        MainDB.init(context, mainDbPath)
        SyncDB.init(context, syncDbPath)

        Log.i(logTag, "... Found legacy db files, about to migrate")

        // List existing accounts and migrate them one by one
        val accDB = MainDB.getHelper()
        val syncDB = SyncDB.getHelper()
        val recs = accDB.listAccountRecords()
        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        for (rec in recs) {
            if (rec.isLegacy) {
                migrateOneP8Account(rec, accDB)
            } else {
                migrateOneCellsAccount(rec, accDB, syncDB)
            }
            Log.w(logTag, "- " + rec.username + "@" + rec.url())
        }
    }

    private suspend fun migrateOneCellsAccount(
        record: AccountRecord,
        mainDB: MainDB,
        syncDB: SyncDB
    ) {
        val currState = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $currState")

        val session = accountService.getSession(currState)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val token = mainDB.getToken(record.id())
            if (token == null) {
                val server = sessionFactory.registerServer(serverURL)
                accountService.registerAccount(
                    record.username,
                    server,
                    AppNames.AUTH_STATUS_NO_CREDS
                )
            } else {
                // TODO better handling of expired and error tokens
                val jwtCredentials = JWTCredentials(record.username, token)
                accountService.signUp(serverURL, jwtCredentials)
            }
        }

        val offlineRoots = syncDB.getWatches(record.id())
        if (offlineRoots.isEmpty()) {
            return
        }

        val client = try {
            sessionFactory.getUnlockedClient(currState.accountId)
        } catch (e: Exception) {
            null
        } ?: return

        for (currRoot in offlineRoots) {
            val storedFileNode = currRoot.node
            val state = currState.withPath("/"+ storedFileNode.workspace + storedFileNode.path)
            val newNode = if (client == null) {
                RTreeNode.fromFileNode(state, storedFileNode)
            } else {
                val fn = client.nodeInfo(storedFileNode.workspace, storedFileNode.path)
                RTreeNode.fromFileNode(state, fn)
            }
            nodeService.updateOfflineRoot(newNode)
        }
    }

    private suspend fun migrateOneP8Account(record: AccountRecord, mainDB: MainDB) {
        val currState = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $currState")

        val session = accountService.getSession(currState)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val pwd = mainDB.getPassword(record.id())
            if (pwd == null) {
                val server = sessionFactory.registerServer(serverURL)
                accountService.registerAccount(
                    record.username,
                    server,
                    AppNames.AUTH_STATUS_NO_CREDS
                )
            } else {
                // TODO better handling of expired and error tokens
                val jwtCredentials = LegacyPasswordCredentials(record.username, pwd)
                accountService.signUp(serverURL, jwtCredentials)
            }
        }
    }

}
