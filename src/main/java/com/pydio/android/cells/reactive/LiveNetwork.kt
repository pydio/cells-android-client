package com.pydio.android.cells.reactive

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData

private const val logTag = "LiveNetwork"

/**
 * Relies on the device connectivity manager to expose current Network status as liveData
 * to ease use in the UI layers.
 */
class LiveNetwork(context: Context) : LiveData<NetworkStatus>() {


    private var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    override fun onActive() {
        Log.i(logTag, "onActive()")
        super.onActive()
        connectivityManagerCallback = getConnectivityManagerCallback()
        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)
    }

    override fun onInactive() {
        Log.i(logTag, "onInactive()")
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
    }

    // See https://developer.android.com/training/basics/network-ops/reading-network-state
    // we only use the default network in a first pass
    private fun getConnectivityManagerCallback() = CellsNetworkCallback { postValue(it) }
}

class CellsNetworkCallback(val postValue: (NetworkStatus) -> Unit) :
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
        Log.i(logTag, "   capabilities: $networkCapabilities.")
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
