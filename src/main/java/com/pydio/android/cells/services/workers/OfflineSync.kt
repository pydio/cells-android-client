package com.pydio.android.cells.services.workers

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.utils.fromFreqToMinuteInterval
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class OfflineSync(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val prefs: PreferencesService by inject()
    private val offlineService: OfflineService by inject()

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
        private const val logTag = "OfflineSync"

        suspend fun buildWorkRequest(prefs: PreferencesService): PeriodicWorkRequest {

            // Build constraints based on preferences
            val constraintBuilder = Constraints.Builder()

            val settings = prefs.fetchPreferences().sync

            val netType = when (settings.onNetworkType) {
                AppNames.NETWORK_TYPE_CONSTRAINT_NOT_ROAMING -> NetworkType.NOT_ROAMING
                AppNames.NETWORK_TYPE_CONSTRAINT_NONE -> NetworkType.CONNECTED
                else -> NetworkType.UNMETERED
            }
            constraintBuilder.setRequiredNetworkType(netType)

            if (settings.onCharging) {
                constraintBuilder.setRequiresCharging(true)
            }

            if (settings.onBatteryNotLow) {
                constraintBuilder.setRequiresBatteryNotLow(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && settings.onIdle) {
                constraintBuilder.setRequiresDeviceIdle(true)
            }

            val repeatInterval = fromFreqToMinuteInterval(settings.frequency)
            Log.e(logTag, "... Built offline request with a frequency of $repeatInterval min.")
            return PeriodicWorkRequestBuilder<OfflineSync>(
                repeatInterval, TimeUnit.MINUTES
            ).setConstraints(constraintBuilder.build()).build()
        }
    }

    override suspend fun doWork(): Result {

        if (hasBeenMigrated()) {
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