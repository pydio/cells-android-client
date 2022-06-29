package com.pydio.android.cells.reactive

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class LiveNetwork(context: Context) : LiveData<NetworkStatus>() {

    private val logTag = LiveNetwork::class.simpleName

    val validNetworkConnections: MutableSet<Network> = mutableSetOf()
    val unMeteredConnections: MutableSet<Network> = mutableSetOf()

    var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    fun announceStatus() {
        Log.d(
            logTag, "About to announce status for $this, " +
                    "we have ${validNetworkConnections.size} valid connections" +
                    " (${unMeteredConnections.size} un-metered)"
        )

//        Log.e(logTag, "Browsing all con:")
//        for (currCon in validNetworkConnections) {
//            Log.e(logTag, "${currCon.networkHandle}")
//        }

        if (unMeteredConnections.isNotEmpty()) {
            postValue(NetworkStatus.Available)
        } else if (validNetworkConnections.isNotEmpty()) {
            postValue(NetworkStatus.Metered)
        } else {
            postValue(NetworkStatus.Unavailable)
        }
    }

    override fun onActive() {
        Log.i(logTag, "### $this.onActive(). We have ${validNetworkConnections.size} valid con")
        super.onActive()
        connectivityManagerCallback = getConnectivityManagerCallback()
        val networkRequest = NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, connectivityManagerCallback)
        announceStatus()
    }

    override fun onInactive() {
        Log.i(logTag, "onInactive()")
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
    }

    private fun getConnectivityManagerCallback() =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val networkCapability = connectivityManager.getNetworkCapabilities(network)
                val hasNetworkConnection =
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        ?: false
                if (hasNetworkConnection) {
                    determineInternetAccess(network, networkCapability)
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                validNetworkConnections.remove(network)
                unMeteredConnections.remove(network)
                announceStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    determineInternetAccess(network, networkCapabilities)
                } else {
                    validNetworkConnections.remove(network)
                    unMeteredConnections.remove(network)
                }
                announceStatus()
            }
        }

    private fun determineInternetAccess(
        network: Network, networkCapabilities: NetworkCapabilities?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (InternetAvailability.check(network)) {
                withContext(Dispatchers.Main) {
                    validNetworkConnections.add(network)
                    if (networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
                    ) {
                        unMeteredConnections.add(network)
                    }
                    announceStatus()
                }
            }
        }
    }
}

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Metered : NetworkStatus()
    object Unavailable : NetworkStatus()
}

private object InternetAvailability {

    // private val logTag = InternetAvailability::class.simpleName

    fun check(network: Network): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53))
            socket.close()

            // TODO should we really check Google DNS?
//            val pydioUrl = ServerURLImpl.fromAddress("https://files.example.com")
//            try {
//                Log.e(logTag, "About to ping: $network")
//                pydioUrl.ping()
//                Log.e(logTag, "Ping succeed")
//                true
//            } catch (e: Exception) {
//                e.printStackTrace()
//                false
//            }
//            Log.e(logTag, "Checking internet connectivity for: ${network.toString()}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
