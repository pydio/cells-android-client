package com.pydio.android.legacy.v2

import android.content.Context
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.runtime.LogDao
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampForLogMessage
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ServerURL
import com.pydio.cells.api.callbacks.ProgressListener
import com.pydio.cells.legacy.P8Credentials
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

    private val logTag = "MigrationServiceV2"

    private val accountService by inject<AccountService>()
    private val sessionFactory by inject<SessionFactory>()
    // private val nodeService by inject<NodeService>()
    // private val prefs: PreferencesService by inject()

    private val jobService by inject<JobService>()
    private val offlineService by inject<OfflineService>()
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
    suspend fun migrate(context: Context, migrationJob: RJob, oldValue: Int, newValue: Int): Int {

//        // FIXME
//        for (i in 1..20) {
//            Log.e(logTag, "Preparing step #$i ...")
//            jobService.incrementProgress(migrationJob, 5, "Preparing step #$i ...")
//            delay(1500)
//        }
//        return 8

        delay(1200) // dirty workaround: take a nap even if you're a speedy device

        val result: Pair<Boolean, Int>
        val timeToSync = measureTimedValue {

            var beginTS = currentTimestamp()
            jobService.incrementProgress(migrationJob, 0, "Preparing migration...")
            prepare(context)
            var dur = currentTimestamp() - beginTS
            if (dur < 1000) {
                delay(1200 - dur)
            }
            jobService.incrementProgress(migrationJob, 20, "Migrating accounts and credentials...")

            result = try {
                if (oldValue < 50) {
                    migrateAccountsFromV23x(context, migrationJob, oldValue, newValue, 50) {
                        jobService.incrementProgress(migrationJob, it, null)
                        ""
                    }
                } else {
                    migrateAccountsFromV24x(migrationJob, oldValue, newValue, 50) {
                        jobService.incrementProgress(migrationJob, it, null)
                        ""
                    }
                }
            } catch (e: Exception) {
                jobService.failed(
                    migrationJob.jobId,
                    "could not perform migration from code version $oldValue to $newValue, cause: ${e.message} "
                )
                return 0
            }

            beginTS = currentTimestamp()
            if (result.first) {
                jobService.incrementProgress(migrationJob, 0, "Cleaning legacy files...")
                cleanLegacyFiles(context)
            }
            dur = currentTimestamp() - beginTS
            if (dur < 1000) {
                delay(1200 - dur)
            }
        }

        if (result.first) {
            val msg =
                "Migration done with ${result.second} offline roots in ${timeToSync.duration.inWholeSeconds}s"
            val progressMsg = "Migration terminated on ${timestampForLogMessage()}"
            jobService.done(migrationJob, msg, progressMsg)
        } else {
            jobService.failed(
                migrationJob.jobId,
                "Unexpected error while trying to migrate from $oldValue to $newValue"
            )
        }
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
    private suspend fun migrateAccountsFromV23x(
        context: Context,
        job: RJob,
        oldValue: Int,
        newValue: Int,
        maxProgress: Int,
        progressListener: ProgressListener
    ): Pair<Boolean, Int> {
        val accDB = V2MainDB.helper ?: return false to 0
        val syncDB = V2SyncDB.helper ?: return false to 0

        // Load accounts that have been already registered by the user
        val recs = accDB.listLegacyAccountRecords()

        if (recs.isEmpty()) {
            val msg = "No account found. Nothing to migrate..."
            jobService.w(logTag, msg, "${job.jobId}")
            return Pair(true, 0)
        }

        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        val oneStep = maxProgress / recs.size
        var offlineRootsNb = 0

        // Convert each account to new format recreate mapping with tokens
        for (rec in recs) {
            val beginTS = currentTimestamp()
            try {
                val accountID = StateID(rec.user, rec.server.url)
                Log.i(logTag, "About to migrate: $accountID from $oldValue to $newValue")

                val url = ServerURLImpl.fromAddress(rec.server.url, rec.server.sslUnverified)

                if (rec.server.versionName == "cells") {
                    migrateCellsFrom23x(accDB, rec.user, url)
                } else {
                    migrateP8From23x(rec, accDB, url)
                }

                offlineRootsNb += refreshMigratedAccount(job, accountID, rec.ID, syncDB)

                // We will loose the info of the old ID and thus the path after this point,
                // So for old legacy instance, we must clean folders here
                Log.e(logTag, "About to delete files for ID: " + rec.ID)

                val file = File(context.filesDir.absoluteFile, rec.ID)
                if (file.exists()) {
                    file.deleteRecursively()
                }

                progressListener.onProgress(oneStep.toLong())
                val msg = "$accountID has been migrated"
                jobService.i(logTag, msg, "${job.jobId}")
            } catch (e: Exception) { // We have a NPE from production here. Try to gather more info
                val msg = "could not migrate ${rec.user}@${rec.server.url}: ${e.message}"
                jobService.e(logTag, msg, "${job.jobId}", e)
                return Pair(false, 0)
            }
            val dur = currentTimestamp() - beginTS
            if (dur < 1000) {
                delay(1000 - dur)
            }
        }
        return Pair(true, offlineRootsNb)
    }

    // List existing accounts and migrate them one by one
    private suspend fun migrateAccountsFromV24x(
        job: RJob,
        oldValue: Int,
        newValue: Int,
        maxProgress: Int,
        progressListener: ProgressListener
    ): Pair<Boolean, Int> {
        val accDB = V2MainDB.helper ?: return false to 0
        val syncDB = V2SyncDB.helper ?: return false to 0


        val recs = accDB.listAccountRecords()
        if (recs.isEmpty()) {
            val msg = "No account found. Nothing to migrate..."
            Log.w(logTag, "    $msg")
            logDao.insert(RLog.info(logTag, msg, null))
            jobService.w(logTag, msg, "${job.jobId}")
            return Pair(true, 0)
        }

        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        val oneStep = maxProgress / recs.size
        var offlineRootsNb = 0
        for (rec in recs) {
            val beginTS = currentTimestamp()
            try {

                val accountID = StateID.fromId(rec.id())
                Log.i(logTag, "About to migrate: $accountID from $oldValue to $newValue")
                val session = accountService.getSession(accountID)

                if (session == null) {
                    if (rec.isLegacy) {
                        migrateP8From24x(rec, accDB)
                    } else {
                        migrateCellsFrom24x(rec, accDB)
                    }
                }

                offlineRootsNb += refreshMigratedAccount(job, accountID, rec.id(), syncDB)

                progressListener.onProgress(oneStep.toLong())
                val msg = "${rec.username}@${rec.url()} has been migrated"
                jobService.i(logTag, msg, "${job.jobId}")
            } catch (e: Exception) {
                val msg = "could not migrate ${rec.username}@${rec.url()}: ${e.message}"
                jobService.e(logTag, msg, "${job.jobId}", e)
                return Pair(false, 0)
            }
            val dur = currentTimestamp() - beginTS
            if (dur < 1000) {
                delay(1000 - dur)
            }
        }
        return Pair(true, offlineRootsNb)
    }

    /** @return the number of migrated offline roots */
    private suspend fun refreshMigratedAccount(
        job: RJob,
        accountID: StateID,
        oldId: String, // record.id()
        syncDB: V2SyncDB
    ): Int {
        // Refresh workspace list and check credentials
        val client = try {
            sessionFactory.getUnlockedClient(accountID.accountId)
        } catch (e: Exception) {
            val msg = "could not retrieve client for $accountID: ${e.message}"
            jobService.e(logTag, msg, "${job.jobId}")
            Log.e(logTag, msg)
            e.printStackTrace()
            return 0
        }

        val (_, errMsg) = accountService.refreshWorkspaceList(accountID.account())
        errMsg?.let {
            val msg = "could not list workspaces for $accountID: $errMsg"
            Log.w(logTag, msg)
            jobService.w(logTag, msg, "${job.jobId}")
            return 0
        }

        // We do not support offline roots for P8
        if (client.isLegacy) {
            return 0
        }

        // Migrate offline roots
        val offlineRoots = syncDB.getWatches(oldId)
        if (offlineRoots.isEmpty()) {
            return 0
        }

        for (currRoot in offlineRoots) {
            val storedFileNode = currRoot.node
            val state = accountID.withPath("/" + storedFileNode.workspace + storedFileNode.path)
            val newNode = if (Str.empty(storedFileNode.mimeType)) {
                storedFileNode.setProperty(
                    SdkNames.NODE_PROPERTY_MIME,
                    SdkNames.NODE_MIME_DEFAULT
                )
                RTreeNode.fromFileNode(state, storedFileNode)
            } else {
                val fn = client.nodeInfo(storedFileNode.workspace, storedFileNode.path)
                RTreeNode.fromFileNode(state, fn)
            }
            offlineService.updateOfflineRoot(newNode, AppNames.OFFLINE_STATUS_MIGRATED)
        }

        return offlineRoots.size
    }

    // TODO better handling of expired and error tokens

    private suspend fun migrateCellsFrom23x(
        v2MainDB: V2MainDB,
        userName: String,
        serverURL: ServerURL
    ) {
        val subject = userName + "@" + serverURL.url
        val token = v2MainDB.getToken(subject)
        if (token == null) {
            val server = sessionFactory.registerServer(serverURL)
            accountService.registerAccount(userName, server, AppNames.AUTH_STATUS_NO_CREDS)
        } else {
            val jwtCredentials = JWTCredentials(userName, token)
            accountService.signUp(serverURL, jwtCredentials)
        }
    }

    private suspend fun migrateP8From23x(
        record: LegacyAccountRecord,
        v2MainDB: V2MainDB,
        serverURL: ServerURL
    ) {
        val subject = record.user + "@" + serverURL.url
        val pwd = v2MainDB.getPassword(subject)
        if (pwd == null) {
            val server = sessionFactory.registerServer(serverURL)
            accountService.registerAccount(record.user, server, AppNames.AUTH_STATUS_NO_CREDS)
        } else {
            val jwtCredentials = P8Credentials(record.user, pwd)
            accountService.signUp(serverURL, jwtCredentials)
        }
    }

    private suspend fun migrateCellsFrom24x(
        record: AccountRecord,
        v2MainDB: V2MainDB
    ) {
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
            val jwtCredentials = JWTCredentials(record.username, token)
            accountService.signUp(serverURL, jwtCredentials)
        }
    }

    private suspend fun migrateP8From24x(
        record: AccountRecord,
        v2MainDB: V2MainDB
    ) {
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
            val jwtCredentials = P8Credentials(record.username, pwd)
            accountService.signUp(serverURL, jwtCredentials)
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

    private suspend fun cleanLegacyFiles(context: Context) {
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
