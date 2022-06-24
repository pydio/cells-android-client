package com.pydio.android.cells.services

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
import com.pydio.android.cells.utils.fromFreqToMinuteInterval
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class OfflineSyncWorker(
    private val accountService: AccountService,
    private val nodeService: NodeService,
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {


    private val prefs: CellsPreferences by inject()

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
        private val logTag = OfflineSyncWorker::class.simpleName



        fun buildWorkRequest(prefs: CellsPreferences): PeriodicWorkRequest {

//            val logTag = "buildWorkRequest"

            val frequency = prefs.getString(
                AppNames.PREF_KEY_OFFLINE_FREQ,
                AppNames.OFFLINE_FREQ_WEEK
            )
            val onWifi = prefs.getBoolean(AppNames.PREF_KEY_OFFLINE_CONST_WIFI, true)
            val onCharging = prefs.getBoolean(AppNames.PREF_KEY_OFFLINE_CONST_CHARGING, true)

            // TODO Improve this:
            //   - on wifi is no equivalent to !onMetered
            //   - re-add requires device Idle true

            // Useful worker
            val builder = Constraints.Builder().setRequiresBatteryNotLow(true)

            if (onWifi) {
                builder.setRequiredNetworkType(NetworkType.UNMETERED)
            }
            if (onCharging) {
                builder.setRequiresCharging(true)
            }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            builder.setRequiresDeviceIdle(true)
//        }
// alternative (more elegant?) syntax:
            //            .apply {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    setRequiresDeviceIdle(true)
//                }
//            }

            // Dev constraints. Do **not** use this in production
//        val repeatingRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
//            16,
//            TimeUnit.MINUTES
//        ).setConstraints(builder.build())
//            .build()

            val repeatInterval = fromFreqToMinuteInterval(frequency)
            Log.d(logTag, "... Built offline request with a frequency of $repeatInterval min.")

            return PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                repeatInterval, TimeUnit.MINUTES
            ).setConstraints(builder.build()).build()

        }


    }

    override suspend fun doWork(): Result {

        if (hasBeenMigrated()){
            nodeService.runFullSync("Worker")
        }

        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }

    // Dirty tweak to prevent the first sync to be launched before the migration from v2 has correctly run
    private fun hasBeenMigrated(): Boolean{
        return prefs.getInt(AppNames.PREF_KEY_INSTALLED_VERSION_CODE) > 107
    }

}
