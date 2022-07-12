package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.LiveNetwork
import com.pydio.android.cells.reactive.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class NetworkService constructor(context: Context) {

    private val logTag = NetworkService::class.simpleName
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Business objects
    private var _networkStatus: NetworkStatus = NetworkStatus.Unknown
    val networkStatus: NetworkStatus
        get() = _networkStatus

    private val _networkType = MutableLiveData(AppNames.NETWORK_TYPE_UNMETERED)
    val networkType: LiveData<String>
        get() = _networkType

    // Manage UI
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        serviceScope.launch { // Asynchronous is necessary to wait for the context
            // Default are rather optimistic, otherwise we get some UI glitches while the app starts
            _networkType.value = AppNames.NETWORK_TYPE_UNMETERED

            val liveNetwork = LiveNetwork(context)
            setNetworkStatus(liveNetwork.value ?: NetworkStatus.Unknown)

            liveNetwork.asFlow().collect {
                setNetworkStatus(it)
            }
            Log.i(logTag, "Initial status: ${liveNetwork.value}")
            Log.i(logTag, "After init, current network status: $_networkType")
        }
    }

    fun isConnected(): Boolean {
        return _networkStatus is NetworkStatus.Unmetered ||
                _networkStatus is NetworkStatus.Metered ||
                _networkStatus is NetworkStatus.Roaming
    }

    fun isMetered(): Boolean {
        return _networkStatus is NetworkStatus.Metered ||
                _networkStatus is NetworkStatus.Roaming
    }

    private fun setNetworkStatus(status: NetworkStatus) {

        Log.e(logTag, "### Setting new status: $status")
        this._networkStatus = status

        serviceScope.launch(Dispatchers.Main) {
            _networkType.value = when (status) {
                is NetworkStatus.Unmetered -> AppNames.NETWORK_TYPE_UNMETERED
                is NetworkStatus.Metered -> AppNames.NETWORK_TYPE_METERED
                is NetworkStatus.Roaming -> AppNames.NETWORK_TYPE_ROAMING
                is NetworkStatus.Unavailable -> AppNames.NETWORK_TYPE_UNAVAILABLE
                else -> AppNames.NETWORK_TYPE_UNKNOWN
            }
        }
    }

// TODO moved here from main activity.
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
            Log.d(
                logTag, String.format(
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
