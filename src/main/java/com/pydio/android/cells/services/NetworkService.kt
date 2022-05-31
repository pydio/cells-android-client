package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import com.pydio.android.cells.db.runtime.NetworkInfoDao
import com.pydio.android.cells.db.runtime.RNetworkInfo
import com.pydio.android.cells.utils.currentTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class NetworkService constructor(
    private val context: Context,
    private val networkDao: NetworkInfoDao,
) {

    private val logTag = NetworkService::class.simpleName

    private val networkServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + networkServiceJob)

    init {
        serviceScope.launch {
            networkDao.get() ?: let {
                networkDao.insert(RNetworkInfo())
            }
        }
    }

    fun getLiveStatus(): LiveData<RNetworkInfo> = networkDao.getLive()

    fun networkInfo(): RNetworkInfo? = networkDao.get()

    fun updateStatus(newStatus: String, errorCode: Int) {
        // Log.e(logTag, "!!!!! Updating status to $newStatus")
        // Thread.dumpStack()
        val info = networkDao.get() ?: RNetworkInfo()
        info.status = newStatus
        info.lastCheckedTS = currentTimestamp()
        networkDao.update(info)
    }

    fun isNetworkConnected(): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    fun isNetworkMetered(): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> false
                else -> true
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_MOBILE -> true
                        else -> false
                    }

                }
            }
        }
        return result
    }

    fun hasInternetAccess(): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> true
                else -> false
            }
        } else {
            // TODO implement this if really needed
            return true
        }

        return result
    }

// FIXME moved here from main activity.
    // Not sure if it still works

// This was in MainActivity.OnCreate method
//        NetworkStatusHelper(this@MainActivity).observe(this, {
//            showMessage(
//                this@MainActivity,
//                when (it) {
//                    NetworkStatus.Available -> "Network Connection Established"
//                    NetworkStatus.Unavailable -> "No Internet"
//                }
//            )
//        })

    private fun networkUsage(context: Context) {
        // Get running processes
        // val manager = getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
        val manager = getSystemService(context, ActivityManager::class.java) as ActivityManager

        val runningApps = manager.runningAppProcesses
        for (runningApp in runningApps) {
            val received = TrafficStats.getUidRxBytes(runningApp.uid)
            val sent = TrafficStats.getUidTxBytes(runningApp.uid)
            android.util.Log.d(
                logTag, java.lang.String.format(
                    Locale.getDefault(),
                    "uid: %1d - name: %s: Sent = %1d, Received = %1d",
                    runningApp.uid,
                    runningApp.processName,
                    sent,
                    received
                )
            )
        }
    }
}
