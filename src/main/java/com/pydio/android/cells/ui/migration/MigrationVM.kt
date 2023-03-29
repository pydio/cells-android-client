package com.pydio.android.cells.ui.migration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.runtime.JobDao
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.legacy.v2.MigrationServiceV2
import com.pydio.cells.transport.ClientData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class Step {
    STARTING, MIGRATING_FROM_V2, AFTER_LEGACY_MIGRATION, AFTER_MIGRATION_ERROR, NOT_NEEDED
}

class MigrationVM(
    private val prefs: PreferencesService,
    private val jobService: JobService,
    private val jobDao: JobDao,
    private val offlineService: OfflineService
) : ViewModel() {

    private val logTag = "MigrationVM"
    private val _noJobID = -1L

    private val id: String = UUID.randomUUID().toString()

    val versionCode = prefs.cellsPreferencesFlow.map { it.versionCode }

    private val migrationService = MigrationServiceV2()

    private val _currDestination = MutableStateFlow(Step.STARTING)
    val currDestination: StateFlow<Step>
        get() = _currDestination

    private var _rootNb: Int = 0
    val rootNb: Int
        get() = _rootNb

    private var _migrationJob: LiveData<RJob?> = jobDao.getLiveById(_noJobID)
    val migrationJob: LiveData<RJob?>
        get() = _migrationJob

    init {
        Log.d(logTag, "After Init for #$id")
    }

    override fun onCleared() {
        // useless: this does nothing
        // super.onCleared()
        Log.d(logTag, "Cleared for #$id")
    }

    suspend fun migrate(context: Context) {
        val oldVersion = getOldVersion(context)
        val newVersion = ClientData.getInstance().versionCode.toInt()

        if (!needsMigration(context)) {
            // The finer check we make here tells us we do not need migration in fact
            val newValue = ClientData.getInstance().versionCode.toInt()
            prefs.setInstalledVersion(newValue)
            setStep(Step.NOT_NEEDED)
            return
        }

        Log.e(logTag, "Migration to v3 (from $oldVersion to $newVersion)")

        val migrationJob: RJob? = withContext(Dispatchers.IO) {
            val job = jobService.createAndLaunch(
                AppNames.JOB_OWNER_WORKER,
                AppNames.JOB_TEMPLATE_MIGRATION_V2,
                "Migration to v3 (from $oldVersion to $newVersion)",
                maxSteps = 100
            ) ?: return@withContext null
            jobService.i(logTag, "Created ${job.label}", "${job.jobId}")
            job
        }

        if (migrationJob == null) {
            jobService.e(logTag, "Could not create migration Job")
            setStep(Step.AFTER_MIGRATION_ERROR)
            return
        }

        // We notify the first
        _migrationJob = jobDao.getLiveById(migrationJob.jobId)
        setStep(Step.MIGRATING_FROM_V2)

        Log.e(logTag, "Created job with id: ${migrationJob.jobId}")

        _rootNb = doMigrate(context, migrationJob, oldVersion, newVersion)

        val newValue = ClientData.getInstance().versionCode.toInt()
        prefs.setInstalledVersion(newValue)

        setStep(Step.AFTER_LEGACY_MIGRATION)
        Log.e(logTag, "DB migration done")
    }

    suspend fun launchSync() {
        CellsApp.instance.appScope.launch {
            withContext(Dispatchers.IO) {
                offlineService.runFullSync("${AppNames.JOB_OWNER_USER} (post-migration)")
            }
        }
    }

    private suspend fun needsMigration(context: Context): Boolean {
        val oldValue = getOldVersion(context)
        val newValue = ClientData.getInstance().versionCode.toInt()
        return needsMigration(context, oldValue, newValue)
    }


    private fun setStep(currStep: Step) {
        _currDestination.value = currStep
    }

    private suspend fun doMigrate(
        context: Context,
        migrationJob: RJob,
        oldValue: Int,
        newValue: Int
    ): Int = withContext(Dispatchers.IO) {
        val nb = migrationService.migrate(context, migrationJob, oldValue, newValue)
        jobService.i(
            logTag, "${migrationJob.label} terminated",
            "${migrationJob.jobId}"
        )
        // afterMigration(offlineRootsNb)
        nb
    }

    private fun needsMigration(context: Context, oldValue: Int, newValue: Int): Boolean {
        Log.d(logTag, "in needsMigration() - old version: $oldValue, new version: $newValue")

        // Already at the latest version
        if (oldValue == newValue) {
            return false
        }

        // New installation without legacy data
        if (oldValue < 1 && !migrationService.hasLegacyDB(context)) {
            return false
        }

        // No migration is necessary for the time being when coming from a 100+ version
        if (oldValue > 100) {
            return false
        }

        // We probably need a migration but found no legacy DB
        if (!migrationService.hasLegacyDB(context)) {
            val msg = "could not find legacy DB files for version $oldValue, aborting"
            jobService.e(logTag, msg, "-")
            return false
        }

        // Migration needed, we do not update oldValue yet.
        return true
    }

    // Private Helpers

    // Kept to still be able to migrate very old version of the app (v2-)

    private val legacySharedPrefsKey = "Pydio"
    private val legacyOldVersionKey = "version"

    @Suppress("SameParameterValue")
    private fun getLegacyPreference(context: Context, key: String): String? {
        val sp: SharedPreferences = context.getSharedPreferences(
            legacySharedPrefsKey,
            AppCompatActivity.MODE_PRIVATE
        )
        return sp.getString(key, null)
    }

    private suspend fun getOldVersion(context: Context): Int {

        var oldValue = prefs.getInstalledVersion()
        // var oldValue = prefs.getInt(AppKeys.INSTALLED_VERSION_CODE) // this is automatically migrated

        if (oldValue == -1) {// Try old v2 format
            getLegacyPreference(context, legacyOldVersionKey)?.let { oldValue = it.toInt() }
        }
        return oldValue
    }
}
