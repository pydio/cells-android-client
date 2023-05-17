package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

private const val logTag = "NetworkService"

class NetworkService(
    context: Context,
    // coroutineService: CoroutineService,
) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

//    // Local cache to avoid suspend functions while checking current network
//    // TODO not good enough => relies on the fact that the networkStatusFLow that is a **cold** flow
//    //   has already been called
//    private var _networkStatus: NetworkStatus = NetworkStatus.Unmetered
//    private val networkStatus: NetworkStatus
//        get() = _networkStatus

    suspend fun fetchNetworkStatus(): NetworkStatus = networkStatusFlow.first()

    val networkStatusFlow: Flow<NetworkStatus> = callbackFlow {
        connectivityManagerCallback = CellsNetworkCallback {
            Log.e(logTag, "Updating current network status to $it")
//            _networkStatus = it
            trySendBlocking(it)
                .onFailure { throwable ->
                    Log.e(logTag, "Could not emit in flow: ${throwable?.message}")
                }
        }
        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)

//        delay(600)
//        // Force initialisation --> TODO double check and remove if not necessary
//        connectivityManager.activeNetwork?.let { network ->
//            Log.i(logTag, "Initialising network status flow with network $network")
//            connectivityManager.getNetworkCapabilities(network)?.let {
//                val status = fromCapabilities(it)
//                trySend(status)
//            }
//        } ?: run {
//            _networkStatus = NetworkStatus.Unavailable
//            trySend(NetworkStatus.Unavailable)
//            Log.e(logTag, "Initializing with **NO** status")
//        }

        /* Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
        * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
       * In both cases, callback will be properly unregistered. */
        awaitClose {
//            Log.e(logTag, "####################################################")
//            Log.e(logTag, "Current active network: ${connectivityManager.activeNetwork}")
//            Log.e(logTag, "In await close, about to unregister Network Callback")
            try {
                connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
            } catch (e: IllegalArgumentException) { // Sometimes the callback has not been registered fast enough
                Log.e(logTag, "Could not unregister: ${e.message}")
            }
        }
    }

    suspend fun isConnected(): Boolean {
        return isConnected(fetchNetworkStatus())
    }

    private fun isConnected(networkStatus: NetworkStatus): Boolean {
        return when (networkStatus) {
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

//    fun isMetered(): Boolean {
//        return _networkStatus is NetworkStatus.Metered ||
//                _networkStatus is NetworkStatus.Roaming
//    }

    // TODO retrieve and expose App network usage to the end user
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

private class CellsNetworkCallback(val postValue: (NetworkStatus) -> Unit) :
    ConnectivityManager.NetworkCallback() {

    private val logTag = "CellsNetworkCallback"
    override fun onAvailable(network: Network) {
        Log.e(logTag, "## Using network #$network")
        // TODO manage sockets.
    }

    override fun onLost(network: Network) {
        Log.e(logTag, "## After loosing network #$network")
        postValue(NetworkStatus.Unavailable)
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        Log.e(logTag, "## Capability changed for #$network")

        val status = fromCapabilities(networkCapabilities)
        if (NetworkStatus.Unknown == status) {
            Log.w(logTag, "Unexpected status for network #$network")
        }
        postValue(status)
    }
}

fun fromCapabilities(networkCapabilities: NetworkCapabilities): NetworkStatus {
    return if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        Log.i(logTag, "  capabilities: $networkCapabilities.")
        when {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
            -> NetworkStatus.Unmetered

            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            -> NetworkStatus.Roaming

            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            -> NetworkStatus.Metered

            else -> {
                NetworkStatus.Unknown
            }
        }
    } else {
        NetworkStatus.Unavailable
    }

}

sealed class NetworkStatus {
    object Unmetered : NetworkStatus()
    object Metered : NetworkStatus()
    object Roaming : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Unknown : NetworkStatus()
}
