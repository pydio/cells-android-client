package com.pydio.android.cells.reactive

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData

/**
 * Relies on the device connectivity manager to expose current Network status as liveData
 * to ease use in the UI layers.
 */
class LiveNetwork(context: Context) : LiveData<NetworkStatus>() {

    private val logTag = "LiveNetwork"

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
    private fun getConnectivityManagerCallback() = object : ConnectivityManager.NetworkCallback() {

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
            val status =
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
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
                            Log.w(logTag, "Unexpected status for network #$network")
                            NetworkStatus.Unknown
                        }
                    }
                } else {
                    NetworkStatus.Unavailable
                }

            // TODO re-implement a valid check to insure we can really access the internet
            postValue(status)
        }
    }
}

sealed class NetworkStatus {
    object Unmetered : NetworkStatus()
    object Metered : NetworkStatus()
    object Roaming : NetworkStatus()
    object Unavailable : NetworkStatus()
    object Unknown : NetworkStatus()
}
