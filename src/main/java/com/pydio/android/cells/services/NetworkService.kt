package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.CellsNetworkCallback
import com.pydio.android.cells.reactive.LiveNetwork
import com.pydio.android.cells.reactive.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Locale

class NetworkService(
    context: Context,
    coroutineService: CoroutineService,
) {

    private val logTag = "NetworkService"

    private val serviceScope = coroutineService.cellsIoScope

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback


    // Business objects
    private var _networkStatus: NetworkStatus = NetworkStatus.Unavailable
    val networkStatus: NetworkStatus
        get() = _networkStatus

    // Default is rather optimistic, otherwise we get some UI glitches while the app starts
    private val _networkType = MutableLiveData(AppNames.NETWORK_TYPE_UNMETERED)
    val networkType: LiveData<String>
        get() = _networkType
//
//    val _networkTypeFlow: MutableSharedFlow<String> = mutable
//    val networkTypeFlow: Flow<String> = Flow<String>(AppNames.NETWORK_TYPE_UNMETERED)
//        get() = _networkType


    val networkTypeFlow: Flow<NetworkStatus> = callbackFlow {
        connectivityManagerCallback = CellsNetworkCallback {
            trySendBlocking(it)
                .onFailure { throwable ->
                    Log.e(logTag, "Could not emit in flow: ${throwable?.message}")
                }
        }
        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)

        /* Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
        * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
       * In both cases, callback will be properly unregistered. */
        awaitClose { connectivityManager.unregisterNetworkCallback(connectivityManagerCallback) }
    }

//    // Manage UI
//    private val _errorMessage = MutableLiveData<String?>()
//    val errorMessage: LiveData<String?>
//        get() = _errorMessage


    init {
        serviceScope.launch { // Asynchronous is necessary to wait for the context
            Log.i(logTag, "Initialising network watching")
            val liveNetwork = LiveNetwork(context)
            setNetworkStatus(liveNetwork.value ?: NetworkStatus.Unavailable)

            liveNetwork.asFlow().collect {
                // Log.e(logTag, "##############################################")
                Log.d(logTag, "Live network event: $it")
                setNetworkStatus(it)
            }
            Log.i(logTag, "Initial status: ${liveNetwork.value}")
            Log.d(logTag, "After init, current network status: $_networkType")
        }

//        serviceScope.launch {
//            connectivityManagerCallback = CellsNetworkCallback {
//
//            }
//            connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)
//        }
    }


    fun isConnected(): Boolean {
        return when (_networkStatus) {

            is NetworkStatus.Unknown -> {
                Log.w(logTag, "Unknown network status, doing as if connected")
                true
            }

            is NetworkStatus.Unavailable -> { // There is no network connection
                false
            }

            is NetworkStatus.Unmetered,
            is NetworkStatus.Metered,
            is NetworkStatus.Roaming -> true
        }
    }

    fun isMetered(): Boolean {
        return _networkStatus is NetworkStatus.Metered ||
                _networkStatus is NetworkStatus.Roaming
    }

    private fun setNetworkStatus(status: NetworkStatus) {
        Log.i(logTag, "### Setting new status: $status")
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
