package com.pydio.android.cells.services

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private const val logTag = "NetworkService"

class NetworkService(
    context: Context,
    coroutineService: CoroutineService,
) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    private val networkStatusFlowCold: Flow<NetworkStatus> = callbackFlow {
        connectivityManagerCallback = CellsNetworkCallback {
            Log.e(logTag, "Updating current network status to $it")
            trySendBlocking(it)
                .onFailure { throwable ->
                    Log.e(logTag, "Could not emit in flow: ${throwable?.message}")
                }
        }
        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)

        awaitClose {
            try {
                Log.w(logTag, "In await close, about to unregister Network Callback")
                connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
            } catch (e: IllegalArgumentException) { // Sometimes the callback has not been registered fast enough
                Log.e(logTag, "Could not unregister: ${e.message}")
            }
        }
    }

    // We register a hot flow here that can be subscribed to by various callers to avoid
    // systematically register / unregister the above cold flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val networkStatusFlow: StateFlow<NetworkStatus> = networkStatusFlowCold.stateIn(
        scope = coroutineService.cellsIoScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkStatus.Unavailable
    )

    suspend fun fetchNetworkStatus(): NetworkStatus = networkStatusFlow.first()

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

            is NetworkStatus.Captive -> {
                false
            }

            is NetworkStatus.Unmetered,
            is NetworkStatus.Metered,
            is NetworkStatus.Roaming -> true
        }
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
        Log.i(logTag, "## Using network #$network")
        // TODO manage sockets.
    }

    override fun onLost(network: Network) {
        Log.i(logTag, "## After loosing network #$network")
        postValue(NetworkStatus.Unavailable)
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        Log.d(logTag, "## Capability changed for #$network")
        val status = fromCapabilities(networkCapabilities)
        if (NetworkStatus.Unknown == status) {
            Log.w(logTag, "Unexpected status for network #$network")
        }
        postValue(status)
    }
}

fun fromCapabilities(networkCapabilities: NetworkCapabilities): NetworkStatus {
    return if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        Log.d(logTag, ".. capabilities: $networkCapabilities.")
        when {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
            -> NetworkStatus.Captive

            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            -> NetworkStatus.Roaming

            !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            -> NetworkStatus.Metered

            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                    && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    || networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
            -> NetworkStatus.Unmetered

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
    object Captive : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Unknown : NetworkStatus()
}
