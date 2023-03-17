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
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.services.NodeService
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
    private val nodeService: NodeService by inject()

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
        private val logTag = "OfflineSync"

        fun buildWorkRequest(prefs: PreferencesService): PeriodicWorkRequest {

            // Build constraints based on preferences
            val constraintBuilder = Constraints.Builder()

            val netType = when (prefs.getString(
                AppKeys.SYNC_CONST_NETWORK_TYPE,
                AppNames.NETWORK_TYPE_CONSTRAINT_UNMETERED
            )) {
                AppNames.NETWORK_TYPE_CONSTRAINT_NOT_ROAMING -> NetworkType.NOT_ROAMING
                AppNames.NETWORK_TYPE_CONSTRAINT_NONE -> NetworkType.CONNECTED
                else -> NetworkType.UNMETERED
            }
            constraintBuilder.setRequiredNetworkType(netType)

            if (prefs.getBoolean(AppKeys.SYNC_CONST_ON_CHARGING, true)) {
                constraintBuilder.setRequiresCharging(true)
            }

            if (prefs.getBoolean(AppKeys.SYNC_CONST_ON_BATT_NOT_LOW, true)) {
                constraintBuilder.setRequiresBatteryNotLow(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                prefs.getBoolean(AppKeys.SYNC_CONST_ON_IDLE, true)
            ) {
                constraintBuilder.setRequiresDeviceIdle(true)
            }

            val frequency = prefs.getString(AppKeys.SYNC_FREQ, AppNames.SYNC_FREQ_WEEK)
            val repeatInterval = fromFreqToMinuteInterval(frequency)
            Log.d(logTag, "... Built offline request with a frequency of $repeatInterval min.")
            return PeriodicWorkRequestBuilder<OfflineSync>(
                repeatInterval, TimeUnit.MINUTES
            ).setConstraints(constraintBuilder.build()).build()
        }
    }

    override suspend fun doWork(): Result {

        if (hasBeenMigrated()) {
            nodeService.runFullSync("Worker")
        }

        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }

    // Dirty tweak to prevent the first sync to be launched before the migration from v2 has correctly run
    private fun hasBeenMigrated(): Boolean {
        return prefs.getInt(AppKeys.INSTALLED_VERSION_CODE) > 107
    }
}