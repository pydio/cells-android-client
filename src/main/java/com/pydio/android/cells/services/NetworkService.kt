package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
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
    private var _networkStatus: NetworkStatus = NetworkStatus.Unavailable
    val networkStatus: NetworkStatus
        get() = _networkStatus

    private val _isConnected = MutableLiveData<Boolean>()
    val liveInternetFlag: LiveData<Boolean>
        get() = _isConnected
    private val _isMetered = MutableLiveData<Boolean>()
    val liveMeteredFlag: LiveData<Boolean>
        get() = _isMetered

    // Manage UI
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        // Default are rather optimistic, otherwise we get some UI glitches while the framework starts
        _isConnected.value = true
        _isMetered.value = false

        serviceScope.launch { // Asynchronous is necessary to wait for the context

            val liveNetwork = LiveNetwork(context)
            if (liveNetwork.value is NetworkStatus.Unavailable) {
                setNetworkStatus(NetworkStatus.Unavailable)
            } else if (liveNetwork.value is NetworkStatus.Metered) {
                setNetworkStatus(NetworkStatus.Metered)
            }

            liveNetwork.asFlow().collect() {
                it?.let {
                    // Log.d(logTag, "collected status: $it")
                    setNetworkStatus(it)
                }
            }
            Log.i(logTag, "Initial status: ${liveNetwork.value}")
            Log.i(logTag, "After init, current network status: $_networkStatus")
        }
    }

    fun isConnected(): Boolean {
        return _networkStatus is NetworkStatus.Available || _networkStatus is NetworkStatus.Metered
    }

    fun isMetered(): Boolean {
        return _networkStatus is NetworkStatus.Metered
    }

    private fun setNetworkStatus(status: NetworkStatus) {
        Log.i(logTag, "### Setting new status: $status")
        this._networkStatus = status
        serviceScope.launch(Dispatchers.Main) {
            _isConnected.value = _networkStatus !is NetworkStatus.Unavailable
            _isMetered.value = _networkStatus !is NetworkStatus.Metered
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
