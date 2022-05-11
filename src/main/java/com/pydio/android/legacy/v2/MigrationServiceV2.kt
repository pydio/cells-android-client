package com.pydio.android.legacy.v2

import android.content.Context
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.callbacks.ProgressListener
import com.pydio.cells.legacy.P8Credentials
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Centralize migration process from v2 to v3.
 * We only migrate accounts, credentials and offline-roots.
 * Everything else must be downloaded again.
 */
class MigrationServiceV2 : KoinComponent {

    private val logTag = MigrationServiceV2::class.simpleName

    private val accountService by inject<AccountService>()
    private val sessionFactory by inject<SessionFactory>()
    private val nodeService by inject<NodeService>()

    private val oldDbNames = listOf(
        "cache_database.sqlite",
        "database.sqlite",
        "poll_buffer.sqlite",
        "sync.sqlite",
        "sync_buffer.sqlite",
        "sync_operations.sqlite",
        "sync_tree.sqlite",
        "thumbs.sqlite",
    )

    suspend fun migrateOneCellsAccount(
        record: AccountRecord,
        mainDB: MainDB,
        syncDB: SyncDB
    ) {
        val accountID = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $accountID")

        // Main account and credentials
        val session = accountService.getSession(accountID)
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

        // Refresh workspace list and check credentials
        var client = try {
            sessionFactory.getUnlockedClient(accountID.accountId)
        } catch (e: Exception) {
            null
        }

        val result = accountService.refreshWorkspaceList(accountID.accountId)
        if (result.second != null) { // Non-Null response is an error message
            Log.i(logTag, "could not list workspaces for $accountID: ${result.second}")
            client = null
        }

        // Migrate offline roots
        val offlineRoots = syncDB.getWatches(record.id())
        if (offlineRoots.isEmpty()) {
            return
        }

        for (currRoot in offlineRoots) {
            val storedFileNode = currRoot.node
            val state = accountID.withPath("/" + storedFileNode.workspace + storedFileNode.path)
            val newNode = if (client == null) {
                if (Str.empty(storedFileNode.mimeType)) {
                    storedFileNode.setProperty(
                        SdkNames.NODE_PROPERTY_MIME,
                        SdkNames.NODE_MIME_DEFAULT
                    )
                }
                RTreeNode.fromFileNode(state, storedFileNode)
            } else {
                val fn = client.nodeInfo(storedFileNode.workspace, storedFileNode.path)
                RTreeNode.fromFileNode(state, fn)
            }
            nodeService.updateOfflineRoot(newNode, AppNames.OFFLINE_STATUS_MIGRATED)
        }
        // nodeService.syncAll(accountID)
    }

    suspend fun migrateOneP8Account(record: AccountRecord, mainDB: MainDB) {
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
                val jwtCredentials = P8Credentials(record.username, pwd)
                accountService.signUp(serverURL, jwtCredentials)
            }
        }

        // Refresh workspace list to also check credentials
        val result = accountService.refreshWorkspaceList(currState.accountId)
        if (result.second != null) { // Non-Null response is an error message
            Log.w(logTag, "could not list workspaces for $currState: ${result.second}")
        }
    }

    fun doUpload(stateID: StateID, file: File, mime: String) {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            accountService.getClient(stateID).upload(
                inputStream, file.length(),
                mime, stateID.workspace, stateID.file, file.name,
                true, null
            )
        } catch (e: Exception) {
            // TODO manage errors correctly
            Log.e(logTag, "!! could not upload ${file.name}: ${e.message}")
            e.printStackTrace()
        } finally {
            IoHelpers.closeQuietly(inputStream)
        }
    }

    fun prepare(context: Context) {
        // Insure we have all legacy DBs and init
        MainDB.init(context, dbPath(context, MainDB.DB_FILE_NAME))
        SyncDB.init(context, dbPath(context, SyncDB.DB_FILE_NAME))
    }

    // List existing accounts and migrate them one by one
    suspend fun migrateAccounts(maxProgress: Int, progressListener: ProgressListener): Boolean {
        val accDB = MainDB.getHelper()
        val syncDB = SyncDB.getHelper()
        val recs = accDB.listAccountRecords()
        if (recs.size == 0) {
            Log.w(logTag, "    No account found. Nothing to migrate...")
            return true
        }

        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        val oneStep = maxProgress / recs.size
        for (rec in recs) {
            try {
                if (rec.isLegacy) {
                    migrateOneP8Account(rec, accDB)
                } else {
                    migrateOneCellsAccount(rec, accDB, syncDB)
                }
                progressListener.onProgress(oneStep.toLong())
                Log.w(
                    logTag,
                    "... ${rec.username}@${rec.url()} has been migrated"
                )
            } catch (e: Exception) {
                Log.e(
                    logTag,
                    "... could not migrate ${rec.username}@${rec.url()}: ${e.message}"
                )
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    fun cleanLegacyFiles(context: Context) {
        // Deletes all old DB files
        for (name in oldDbNames) {
            rmDB(context, name)
        }

        // Delete old content
        for (account in accountService.listSessionViews(true)) {
            val file = File(context.filesDir.absoluteFile, account.accountID)
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
    }

    private fun rmDB(context: Context, dbFileName: String) {
        val mainDbFile = dbFile(context, dbFileName)
        if (mainDbFile.exists()) {
            mainDbFile.delete()
        }
        val shmDbFile = dbFile(context, "$dbFileName-shm")
        if (shmDbFile.exists()) {
            shmDbFile.delete()
        }
        val walDbFile = dbFile(context, "$dbFileName-wal")
        if (walDbFile.exists()) {
            walDbFile.delete()
        }
        val journalDbFile = dbFile(context, "$dbFileName-journal")
        if (journalDbFile.exists()) {
            journalDbFile.delete()
        }
    }

    fun hasLegacyDB(context: Context): Boolean {
        val mainDbFile = dbFile(context, MainDB.DB_FILE_NAME)
        val syncDbFile = dbFile(context, SyncDB.DB_FILE_NAME)
        return mainDbFile.exists() && syncDbFile.exists()
    }

    fun dbFile(context: Context, name: String): File {
        return File(dbPath(context, name))
    }

    private fun dbPath(context: Context, name: String): String {
        return context.filesDir.absolutePath + File.separator + name
    }
}
