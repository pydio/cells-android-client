package com.pydio.android.legacy.v2

import android.content.Context
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.runtime.LogDao
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.utils.timestampForLogMessage
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.callbacks.ProgressListener
import com.pydio.cells.legacy.P8Credentials
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * Centralize migration process from v2 to v3.
 * We only migrate accounts, credentials and offline-roots.
 * Everything else must be downloaded / cached again.
 */
class MigrationServiceV2 : KoinComponent {

    private val logTag = MigrationServiceV2::class.simpleName

    private val accountService by inject<AccountService>()
    private val sessionFactory by inject<SessionFactory>()
    private val nodeService by inject<NodeService>()
    private val prefs: CellsPreferences by inject()

    private val jobService by inject<JobService>()
    private val logDao by inject<LogDao>()

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

    /**
     * Handle the migration from v2 legacy version to the new v3 versions and model.
     * We add a second of sleep between various steps to be able to see the progress and various
     * status
     *
     * @return the number of offline roots node that have been migrated */
    @OptIn(ExperimentalTime::class)
    suspend fun migrate(context: Context, migrationJob: RJob): Int {

        val result: Pair<Boolean, Int>
        val timeToSync = measureTimedValue {

            jobService.update(migrationJob, 0, "Preparing migration...")

            prepare(context)
            delay(1000)
            jobService.update(migrationJob, 20, "Migrating accounts and credentials...")

            result = migrateAccounts(migrationJob, 50) {
                jobService.update(migrationJob, it, null)
                true
            }
            delay(1000)

            if (result.first) {
                jobService.update(migrationJob, 0, "Cleaning legacy files...")
                cleanLegacyFiles(context)
                prefs.setInt(
                    AppNames.PREF_KEY_INSTALLED_VERSION_CODE,
                    ClientData.getInstance().versionCode.toInt()
                )
            }
            delay(1000)
        }

        val msg =
            "Migration done with ${result.second} offline roots in ${timeToSync.duration.inWholeSeconds}s"
        val progressMsg = "Migration terminated on ${timestampForLogMessage()}"

        jobService.done(migrationJob, msg, progressMsg)
        return result.second
    }

    fun hasLegacyDB(context: Context): Boolean {
        val mainDbFile = dbFile(context, V2MainDB.DB_FILE_NAME)
        val syncDbFile = dbFile(context, V2SyncDB.DB_FILE_NAME)
        return mainDbFile.exists() && syncDbFile.exists()
    }

    fun prepare(context: Context) {
        // Insure we have all legacy DBs and init
        V2MainDB.init(context, dbPath(context, V2MainDB.DB_FILE_NAME))
        V2SyncDB.init(context, dbPath(context, V2SyncDB.DB_FILE_NAME))
    }

    // List existing accounts and migrate them one by one
    private suspend fun migrateAccounts(
        job: RJob,
        maxProgress: Int,
        progressListener: ProgressListener
    ): Pair<Boolean, Int> {
        val accDB = V2MainDB.getHelper()
        val syncDB = V2SyncDB.getHelper()
        val recs = accDB.listAccountRecords()
        if (recs.size == 0) {
            val msg = "No account found. Nothing to migrate..."
            Log.w(logTag, "    $msg")
            logDao.insert(RLog.info(logTag, msg, null))
            return Pair(true, 0)
        }

        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        val oneStep = maxProgress / recs.size
        var offlineRootsNb = 0
        for (rec in recs) {
            try {
                if (rec.isLegacy) {
                    migrateOneP8Account(job, rec, accDB)
                } else {
                    offlineRootsNb += migrateOneCellsAccount(job, rec, accDB, syncDB)
                }
                progressListener.onProgress(oneStep.toLong())
                val msg = "${rec.username}@${rec.url()} has been migrated"
                logDao.insert(RLog.info(logTag, msg, "${job.jobId}"))
                Log.i(logTag, "... $msg")
                delay(2000)
            } catch (e: Exception) {
                Log.e(
                    logTag,
                    "... could not migrate ${rec.username}@${rec.url()}: ${e.message}"
                )
                e.printStackTrace()
                return Pair(false, 0)
            }
        }
        return Pair(true, offlineRootsNb)
    }

    /** @return the number of migrated offline roots */
    suspend fun migrateOneCellsAccount(
        job: RJob,
        record: AccountRecord,
        v2MainDB: V2MainDB,
        syncDB: V2SyncDB
    ): Int {
        val accountID = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $accountID")

        // Main account and credentials
        val session = accountService.getSession(accountID)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val token = v2MainDB.getToken(record.id())
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
            return 0
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

        return offlineRoots.size
        // nodeService.syncAll(accountID)
    }

    suspend fun migrateOneP8Account(job: RJob, record: AccountRecord, v2MainDB: V2MainDB) {
        val currState = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $currState")

        val session = accountService.getSession(currState)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val pwd = v2MainDB.getPassword(record.id())
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

    private fun cleanLegacyFiles(context: Context) {
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


    private fun dbFile(context: Context, name: String): File {
        return File(dbPath(context, name))
    }

    private fun dbPath(context: Context, name: String): String {
        return context.filesDir.absolutePath + File.separator + name
    }
}
