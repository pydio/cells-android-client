package com.pydio.android.cells

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.OfflineSyncWorker
import com.pydio.android.cells.services.allModules
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.ClientData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.inject
import java.util.concurrent.TimeUnit

/**
 * Main entry point of the Pydio client application.
 */
class CellsApp : Application(), KoinComponent {

    private val logTag = CellsApp::class.simpleName

    // Exposed to the whole app for tasks that must survive termination of the calling UI element
    // Typically for actions launched from the "More" menu (copy, move...)
    val appScope = CoroutineScope(SupervisorJob())

    companion object {
        lateinit var instance: CellsApp
            private set
    }

    override fun onCreate() {
        Log.i(logTag, "#################################################################")
        Log.i(logTag, "#########  Launching Cells Android Client application  ##########")
        Log.i(logTag, "#################################################################")
        super.onCreate()
        instance = this

        val currClientData = updateClientData()
        Log.d(logTag, "    Current Client Data: $currClientData")

        // sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        Log.i(logTag, "... Pre-init done")

        // Launch dependency injection framework
        startKoin {
            // androidLogger(Level.DEBUG)
            androidLogger(Level.INFO)
            androidContext(this@CellsApp)
            workManagerFactory()
            modules(allModules)
        }
    }

    @Throws(SDKException::class)
    private fun updateClientData(): String {

        val packageInfo: PackageInfo = try {
            applicationContext.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            throw SDKException("Could not retrieve PackageInfo for $packageName", e)
        }

        val instance = ClientData.getInstance()
        instance.packageID = packageName
        instance.name = resources.getString(R.string.app_name)
        instance.clientID = resources.getString(R.string.client_id)
        instance.buildTimestamp = packageInfo.lastUpdateTime
        instance.version = packageInfo.versionName
        instance.versionCode = compatVersionCode(packageInfo)
        instance.platform = getAndroidVersion()

        ClientData.updateInstance(instance)
        return instance.clientID
    }

    // TODO implement background cleaning, typically:
    //  - states
    //  - upload & downloads

    @Suppress("DEPRECATION")
    private fun compatVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

    private fun getAndroidVersion(): String {
        val release = Build.VERSION.RELEASE
        val sdkVersion = Build.VERSION.SDK_INT
        return "AndroidSDK" + sdkVersion + "v" + release
    }
}

//     /**
//      * If there is a pending work because of previous crash we'd like it to not run.
//      */
//     suspend fun cancelPendingWorkManager(mainApplication: CellsApp) {
//         WorkManager.getInstance(mainApplication).cancelAllWork()
//         // WorkManager.getInstance(mainApplication).cancelAllWork().result.await()

//         // Test launch with one time worker
// //            OneTimeWorkRequestBuilder<OfflineSyncWorker>()
// //                .setInputData(Data.EMPTY)
// //                .build()
// //                .also {
// //                    workManager
// //                        .enqueueUniqueWork(
// //                            OfflineSyncWorker.WORK_NAME + "_" + currentTimestamp(),
// //                            ExistingWorkPolicy.APPEND,
// //                            it
// //                        )
// //                }
// //            Log.e(logTag, "One time OfflineSyncWorker created")
//     }

// }
