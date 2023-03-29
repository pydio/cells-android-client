package com.pydio.android.legacy.v2

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.koin.test.AutoCloseKoinTest
import java.io.File

class LegacyMigrationTest : AutoCloseKoinTest() {

    private val logTag = "LegacyMigrationTest"
    private var doMigrate = false

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Insure we have all legacy DBs and init
        val mainDbPath = context.dataDir.absolutePath + V2MainDB.DB_FILE_PATH
        val mainDbFile = File(mainDbPath)
        val syncDbPath = context.dataDir.absolutePath + V2SyncDB.DB_FILE_PATH
        val syncDbFile = File(syncDbPath)

        doMigrate = if (!mainDbFile.exists() || !syncDbFile.exists()) {
            provisionWithDummyLegacyData(context)
        } else {
            true
        }

        if (doMigrate) {
            V2MainDB.init(context, mainDbPath)
            V2SyncDB.init(context, syncDbPath)
        }
    }

    @Test
    fun migrateAllAccounts() = runTest {

        if (!doMigrate) {
            Log.i(logTag, "... No Legacy data found for migration, aborting")
            return@runTest
        }

        Log.i(logTag, "... Found legacy db files, about to migrate")

        // List existing accounts and migrate them one by one
        val accDB = V2MainDB.helper ?: return@runTest
        val syncDB = V2SyncDB.helper
        val recs = accDB.listAccountRecords()
        Log.w(logTag, "    Found " + recs.size + " accounts. ")
        val migrationServiceV2 = MigrationServiceV2()
        for (rec in recs) {
            try {
                if (rec.isLegacy) {
                    migrationServiceV2.migrateOneP8Account(rec, accDB)
                } else {
                    migrationServiceV2.migrateAndRefreshOneCellsAccount(rec, accDB, syncDB)
                }
                Log.w(logTag, "... ${rec.username}@${rec.url()} has been migrated")
            } catch (e: Exception) {
                Log.e(logTag, "... could not migrate ${rec.username}@${rec.url()}: ${e.message}")
                e.printStackTrace()
            }
        }

        // Helper to easily replay the migration: upload necessary files to one of the newly restored account
        // so that they are available. Put them in resource folder to relaunch the migration on an empty instance.
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//        val targetState = StateID("admin", "https://files.example.com", "/common-files")
//        backupLegacyFiles(context, migrationServiceV2, targetState)
    }

    private fun provisionWithDummyLegacyData(context: Context): Boolean {

        val mainSrcFile =
            FileLoader.getResourceAsFile("/legacy/basic-setup/" + V2MainDB.DB_FILE_NAME)
                ?: return false
        val syncSrcFile =
            FileLoader.getResourceAsFile("/legacy/basic-setup/" + V2SyncDB.DB_FILE_NAME)
                ?: return false

        val mainDbFile = File(context.dataDir.absolutePath + V2MainDB.DB_FILE_PATH)
        mainSrcFile.copyTo(mainDbFile, true)
        val syncDbFile = File(context.dataDir.absolutePath + V2SyncDB.DB_FILE_PATH)
        syncSrcFile.copyTo(syncDbFile, true)

        return true
    }

    private suspend fun uploadFiles(
        context: Context,
        migrationServiceV2: MigrationServiceV2
    ) {
        // Helper to easily replay the migration: upload necessary files to one of the newly restored account
        // so that they are available. Put them in resource folder to relaunch the migration on an empty instance.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val targetState = StateID("admin", "https://files.example.com", "/common-files")
        backupLegacyFiles(context, migrationServiceV2, targetState)
    }

    private suspend fun backupLegacyFiles(
        context: Context,
        migrationServiceV2: MigrationServiceV2,
        targetState: StateID
    ) {
        val mainDbPath = context.dataDir.absolutePath + V2MainDB.DB_FILE_PATH
        val mainDbFile = File(mainDbPath)
        val syncDbPath = context.dataDir.absolutePath + V2SyncDB.DB_FILE_PATH
        val syncDbFile = File(syncDbPath)

        migrationServiceV2.doUpload(targetState, mainDbFile, SdkNames.NODE_MIME_DEFAULT)
        migrationServiceV2.doUpload(targetState, syncDbFile, SdkNames.NODE_MIME_DEFAULT)
    }
}
