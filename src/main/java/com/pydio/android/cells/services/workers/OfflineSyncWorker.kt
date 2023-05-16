package com.pydio.android.cells.services.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.preferences.SyncPreferences
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.utils.fromFreqToMinuteInterval
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val prefs: PreferencesService by inject()
    private val offlineService: OfflineService by inject()

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
        private const val logTag = "OfflineSync"

        /** Build a worker with constraints based on preferences */
        fun buildWorkRequest(pref: SyncPreferences): PeriodicWorkRequest {

            val constraintBuilder = Constraints.Builder()

            val netType = when (pref.onNetworkType) {
                AppNames.NETWORK_TYPE_CONSTRAINT_NOT_ROAMING -> NetworkType.NOT_ROAMING
                AppNames.NETWORK_TYPE_CONSTRAINT_NONE -> NetworkType.CONNECTED
                else -> NetworkType.UNMETERED
            }
            constraintBuilder.setRequiredNetworkType(netType)

            if (pref.onCharging) {
                constraintBuilder.setRequiresCharging(true)
            }

            if (pref.onBatteryNotLow) {
                constraintBuilder.setRequiresBatteryNotLow(true)
            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pref.onIdle) {
//                constraintBuilder.setRequiresDeviceIdle(true)
//            }
            if (pref.onIdle) {
                constraintBuilder.setRequiresDeviceIdle(true)
            }

            val repeatInterval = fromFreqToMinuteInterval(pref.frequency)
            Log.e(logTag, "... Built offline request with a frequency of $repeatInterval min.")
            Log.e(logTag, "... Constraint on network: ${netType.name}")

            return PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                repeatInterval, TimeUnit.MINUTES
            ).setConstraints(constraintBuilder.build()).build()
        }
    }

    override suspend fun doWork(): Result {
        if (hasBeenMigrated()) {
            Log.e(logTag, "... Has been migrated, launching full sync")
            offlineService.runFullSync("Worker")
        }
        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }

    // Dirty tweak to prevent the first sync to be launched before the migration from v2 has correctly run
    private suspend fun hasBeenMigrated(): Boolean {
        return prefs.getInstalledVersion() > 107
    }
}
