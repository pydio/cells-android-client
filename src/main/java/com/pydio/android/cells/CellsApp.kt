package com.pydio.android.cells

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.pydio.android.cells.di.allModules
import com.pydio.android.cells.utils.timestampForLogMessage
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.ClientData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main entry point of the Pydio client application.
 */
class CellsApp : Application(), KoinComponent {

    private val logTag = "CellsApp"

    // Exposed to the whole app for tasks that must survive termination of the calling UI element
    // Typically for actions launched from the "More" menu (copy, move...)
    @Deprecated("Rather us injected @CoroutineService::class")
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

        val userAgent = updateClientData()
        Log.i(logTag, "... $userAgent")
        Log.i(logTag, "... Pre-init done - Timestamp: ${timestampForLogMessage()}")

        startKoin {// Launch dependency injection framework
            androidLogger(Level.INFO)
            androidContext(this@CellsApp)
            workManagerFactory()
            modules(allModules)
        }
    }

    @Throws(SDKException::class)
    private fun updateClientData(): String {

        val packageInfo = internalGetPackageInfo()
        val instance = ClientData.getInstance()

        instance.packageID = packageName
        instance.name = resources.getString(R.string.app_name)
        instance.clientID = resources.getString(R.string.client_id)
        // this is the date when the app has been updated, not the timestamp of the current release
        instance.lastUpdateTime = packageInfo.lastUpdateTime
        // TODO also add a timestamp when releasing
        instance.version = packageInfo.versionName
        instance.versionCode = compatVersionCode(packageInfo)
        instance.platform = getAndroidVersion()
        ClientData.updateInstance(instance)

        return instance.userAgent()
    }

    @Suppress("DEPRECATION")
    // We must explicitly discard warnings when using the old and new version of a given API
    // like below that is only available in v33+ with old version that has already been deprecated
    private fun internalGetPackageInfo(): PackageInfo {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                applicationContext.packageManager.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            throw SDKException("Could not retrieve PackageInfo for $packageName", e)
        }
    }

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
