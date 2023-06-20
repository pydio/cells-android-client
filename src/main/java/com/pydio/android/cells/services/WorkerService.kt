package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.pydio.android.cells.db.preferences.SyncPreferences
import com.pydio.android.cells.services.workers.OfflineSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent

class WorkerService(
    context: Context,
    private val coroutineService: CoroutineService,
    private val prefs: PreferencesService,
    private val jobService: JobService,
) : KoinComponent {

    private val logTag = "WorkerService"
    private val workerScope = CoroutineScope(SupervisorJob())
    private val workManager = WorkManager.getInstance(context)

    private lateinit var oldSyncPrefs: SyncPreferences

    private val syncPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.sync
    }

    init {
        coroutineService.cellsIoScope.launch {
            oldSyncPrefs = prefs.fetchPreferences().sync
            withContext(coroutineService.cpuDispatcher) {
                initOfflineWorkers()
                configureOfflinePrefObserver()
            }
        }
        Log.i(logTag, "## WorkerService initialised")
    }

    // TODO implement background cleaning, typically:
    //  - states
    //  - upload & downloads


    // Insure that the worker is correctly started at launch time
    private fun initOfflineWorkers() {
        workManager.enqueueUniquePeriodicWork(
            OfflineSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            OfflineSyncWorker.buildWorkRequest(oldSyncPrefs),
        )
    }

    private fun configureOfflinePrefObserver() {
        workerScope.launch {
            syncPrefs.collect { currPrefs ->
                Log.e(logTag, "New sync preference value: $currPrefs")
                if (hasPrefChanged(currPrefs)) {
                    resetOfflineWorker(currPrefs)
                }
            }
        }
    }

    private fun resetOfflineWorker(syncPref: SyncPreferences) {
        // Debug info
        val prefix = "### Cancel and restart offline worker with [$syncPref] "
        try {
            jobService.i(logTag, prefix, "SyncPref Observer")
        } catch (e: Exception) {
            Log.e(logTag, "$prefix: could not log with job service: ${e.message}")
        }

        // Effective reset. we might add a "debounce" mechanism
        workManager.cancelUniqueWork(OfflineSyncWorker.WORK_NAME)
        workManager.enqueueUniquePeriodicWork(
            OfflineSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            OfflineSyncWorker.buildWorkRequest(oldSyncPrefs),
        )
    }

    private fun hasPrefChanged(syncPref: SyncPreferences): Boolean {
        val hasChanged = !(syncPref.frequency == oldSyncPrefs.frequency
                && syncPref.onBatteryNotLow == oldSyncPrefs.onBatteryNotLow
                && syncPref.onCharging == oldSyncPrefs.onCharging
                && syncPref.onIdle == oldSyncPrefs.onIdle
                && syncPref.onNetworkType == oldSyncPrefs.onNetworkType)

        if (hasChanged) {
            oldSyncPrefs = syncPref
        }
        return hasChanged
    }

    /**
     * If there is a pending work because of previous crash we'd like it to not run.
     */
//    private suspend fun cancelPendingWorkManager(manager: WorkManager) {
//        Log.e(logTag, ".... cancelPendingWorkManager")
//
//        manager.cancelAllWork()
//        // manager.cancelAllWork().result.await()
//
//        // Test launch with one time worker
//        //            OneTimeWorkRequestBuilder<OfflineSyncWorker>()
//        //                .setInputData(Data.EMPTY)
//        //                .build()
//        //                .also {
//        //                    workManager
//        //                        .enqueueUniqueWork(
//        //                            OfflineSyncWorker.WORK_NAME + "_" + currentTimestamp(),
//        //                            ExistingWorkPolicy.APPEND,
//        //                            it
//        //                        )
//        //                }
//        Log.e(logTag, "One time OfflineSyncWorker created")
//    }
//
}
