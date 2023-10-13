package com.pydio.android.cells.ui.migration

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.legacy.v2.MigrationServiceV2
import com.pydio.cells.transport.ClientData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class Step {
    STARTING, MIGRATING_FROM_V2, AFTER_LEGACY_MIGRATION, AFTER_MIGRATION_ERROR, NOT_NEEDED
}

class MigrationVM(
    private val prefs: PreferencesService,
    private val coroutineService: CoroutineService,
    private val jobService: JobService,
    private val offlineService: OfflineService
) : ViewModel() {

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "MigrationVM_${id.substring(30)}"

    private val _jobID: MutableStateFlow<Long> = MutableStateFlow(-1L)

    val versionCode = prefs.cellsPreferencesFlow.map { it.versionCode }

    private val migrationService = MigrationServiceV2()

    private val _currDestination = MutableStateFlow(Step.STARTING)
    val currDestination: StateFlow<Step>
        get() = _currDestination

    private var _rootNb: Int = 0
    val rootNb: Int
        get() = _rootNb

    @OptIn(ExperimentalCoroutinesApi::class)
    val migrationJob: Flow<RJob?> = _jobID.flatMapLatest { currID ->
        jobService.getLiveJobByID(currID)
    }

    init {
        Log.d(logTag, "Initialised")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(logTag, "After Clear")
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
            val label = "Migration to v3 (from $oldVersion to $newVersion)"
            val jobID = jobService.createAndLaunch(
                AppNames.JOB_OWNER_WORKER,
                AppNames.JOB_TEMPLATE_MIGRATION_V2,
                label,
                maxSteps = 100
            )
            jobService.i(logTag, "Created $label", "Job #$jobID")
            jobService.get(jobID)
        }

        if (migrationJob == null) {
            jobService.e(logTag, "Could not create migration Job")
            setStep(Step.AFTER_MIGRATION_ERROR)
            return
        }

        // We notify the first
        _jobID.value = migrationJob.jobId
        setStep(Step.MIGRATING_FROM_V2)

        viewModelScope.launch {
            _rootNb = doMigrate(context, viewModelScope, migrationJob.jobId, oldVersion, newVersion)
            val newValue = ClientData.getInstance().versionCode.toInt()
            prefs.setInstalledVersion(newValue)
            setStep(Step.AFTER_LEGACY_MIGRATION)
            Log.i(logTag, "Migration has been done")
        }

        Log.i(logTag, "Created migration job with id: ${migrationJob.jobId}")

    }

    suspend fun launchSync() {
        coroutineService.cellsIoScope.launch {
            offlineService.runFullSync("${AppNames.JOB_OWNER_USER} (post-migration)")
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
        scope: CoroutineScope,
        migrationJobID: Long,
        oldValue: Int,
        newValue: Int
    ): Int = withContext(Dispatchers.IO) {
        val nb = migrationService.migrate(context, scope, migrationJobID, oldValue, newValue)
        // TODO retrieve label
//        jobService.i(
//            logTag, "${migrationJob.label} terminated",
//            "${migrationJob.jobId}"
//        )
        jobService.i(logTag, "Job $migrationJobID terminated", "$migrationJobID")
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
