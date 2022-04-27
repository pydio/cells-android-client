package com.pydio.android.cells

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.pydio.android.cells.services.OfflineSyncWorker
import com.pydio.android.cells.services.allModules
import java.util.concurrent.TimeUnit

/**
 * Main entry point of the Pydio client application.
 */
class CellsApp : Application(), KoinComponent {

    private val logTag = CellsApp::class.simpleName

    // Exposed to the whole app for tasks that must survive termination of the calling UI element
    // Typically for actions launched from the "More" menu (copy, move...)
    val appScope = CoroutineScope(SupervisorJob())

    // Shortcut to access preferences, TODO must be a better way to do and let koin handle context injection
    lateinit var sharedPreferences: SharedPreferences

    // var currentTheme = R.style.Theme_Cells

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

        updateClientData()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        Log.i(logTag, "... Pre-init done")

        // Launch dependency injection framework
        startKoin {
            // androidLogger(Level.DEBUG)
            androidLogger(Level.INFO)
            androidContext(this@CellsApp)
            workManagerFactory()
            modules(allModules)
        }

        cancelPendingWorkManager(this)
        setupOfflineWorker(this)
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

    fun getPreference(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun setPreference(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    fun getCurrentState(): StateID? {
        return getPreference(AppNames.PREF_KEY_CURRENT_STATE)?.let { StateID.fromId(it) }
    }

    fun setCurrentState(state: StateID) {
        setPreference(AppNames.PREF_KEY_CURRENT_STATE, state.id)
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

fun setupOfflineWorker(mainApplication: CellsApp) {

    runBlocking {
        val workManager = WorkManager.getInstance(mainApplication)

        // Useful worker
        // TODO make this configurable
        val constraints = Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.UNMETERED)
//                .setRequiresCharging(true)
            .setRequiresBatteryNotLow(true)
//            .apply {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    setRequiresDeviceIdle(true)
//                }
//            }
            .build()


        // Dev constraints. Do **not** use this in production
        val repeatingRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            16,
            TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

//        val repeatingRequest2 = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
//            1, TimeUnit.DAYS
//        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            OfflineSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
        Log.e(mainApplication::class.simpleName + ":setup", "Periodic OfflineSyncWorker created")
    }
}

/**
 * If there is a pending work because of previous crash we'd like it to not run.
 */
private fun cancelPendingWorkManager(mainApplication: CellsApp) {
    runBlocking {
        WorkManager.getInstance(mainApplication).cancelAllWork()
        // WorkManager.getInstance(mainApplication).cancelAllWork().result.await()

        // Test launch with one time worker
//            OneTimeWorkRequestBuilder<OfflineSyncWorker>()
//                .setInputData(Data.EMPTY)
//                .build()
//                .also {
//                    workManager
//                        .enqueueUniqueWork(
//                            OfflineSyncWorker.WORK_NAME + "_" + currentTimestamp(),
//                            ExistingWorkPolicy.APPEND,
//                            it
//                        )
//                }
//            Log.e(logTag, "One time OfflineSyncWorker created")
    }
}