package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.pydio.android.cells.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

private const val LOG_TAG = "NetworkService.kt"

class NetworkService(
    context: Context,
    coroutineService: CoroutineService,
) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    val networkStatusFlowCold: Flow<NetworkStatus> = callbackFlow {
        connectivityManagerCallback = CellsNetworkCallback {
            Log.d(LOG_TAG, ".. Updating current network status to $it")
            trySendBlocking(it)
                .onFailure { throwable ->
                    Log.e(LOG_TAG, "Could not emit in flow: ${throwable?.message}")
                }
        }
        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)

        awaitClose {
            try {
                Log.w(LOG_TAG, "In awaitClose, about to unregister Network Callback")
                connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
            } catch (e: IllegalArgumentException) { // Sometimes the callback has not been registered fast enough
                Log.e(LOG_TAG, "Could not unregister: ${e.message}")
            }
        }
    }

    // We register a hot flow here that can be subscribed to by various callers to avoid
    // systematically register / unregister the above cold flow
    val networkStatusFlow: StateFlow<NetworkStatus> = networkStatusFlowCold.stateIn(
        scope = coroutineService.cellsIoScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkStatus.UNAVAILABLE
    )

    suspend fun isConnected(): Boolean {
        // TODO insure that the first() methods here returns what we expect, a.k.a the current network status
        val networkStatus: NetworkStatus = networkStatusFlow.first()
        return networkStatus.isConnected()
    }

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
                LOG_TAG, String.format(
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
        Log.i(logTag, "## Using network #$network")
        // TODO manage sockets.
    }

    override fun onLost(network: Network) {
        Log.i(logTag, "## After loosing network #$network")
        postValue(NetworkStatus.UNAVAILABLE)
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        Log.d(logTag, "## Capability changed for #$network")
        val status = fromCapabilities(networkCapabilities)
        if (NetworkStatus.UNKNOWN == status) {
            Log.w(logTag, "Unexpected status for network #$network")
        }
        postValue(status)
    }

    private fun fromCapabilities(networkCapabilities: NetworkCapabilities): NetworkStatus {
        return if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            Log.d(LOG_TAG, ".. capabilities: $networkCapabilities.")
            when {
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
                -> NetworkStatus.CAPTIVE

                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                -> NetworkStatus.ROAMING

                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                -> NetworkStatus.METERED

                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                        && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                -> NetworkStatus.OK

                else -> {
                    NetworkStatus.UNKNOWN
                }
            }
        } else {
            NetworkStatus.UNAVAILABLE
        }
    }
}
