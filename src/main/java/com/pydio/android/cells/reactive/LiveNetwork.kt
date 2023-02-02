package com.pydio.android.cells.reactive

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData

class LiveNetwork(context: Context) : LiveData<NetworkStatus>() {

    private val logTag = LiveNetwork::class.simpleName

    private var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    override fun onActive() {
        super.onActive()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManagerCallback = getConnectivityManagerCallback()
            connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)
        } else {
            connectivityManagerCallback = getLegacyCallback()

            val networkRequest = NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, connectivityManagerCallback)
        }
    }

    override fun onInactive() {
        Log.i(logTag, "onInactive()")
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
    }

    private fun announceStatus(network: Network, status: NetworkStatus) {
        Log.d(logTag, "Using network $network")
        Log.d(
            logTag, "we have ${validNetworkConnections.size} live connections " +
                    "(${unMeteredConnections.size} un-metered and ${notRoamingConnections.size} not roaming )"
        )
        postValue(status)
    }

    val validNetworkConnections: MutableSet<Network> = mutableSetOf()
    val unMeteredConnections: MutableSet<Network> = mutableSetOf()
    val notRoamingConnections: MutableSet<Network> = mutableSetOf()

    private fun announceLegacyStatus() {
        Log.d(
            logTag, "About to announce status for $this, " +
                    "we have ${validNetworkConnections.size} live connections" +
                    " (${unMeteredConnections.size} un-metered and ${notRoamingConnections.size} not roaming )"
        )

        val status = when {
            unMeteredConnections.isNotEmpty()
            -> NetworkStatus.Unmetered
            notRoamingConnections.isNotEmpty()
            -> NetworkStatus.Metered
            validNetworkConnections.isNotEmpty()
            -> NetworkStatus.Roaming
            else -> NetworkStatus.Unavailable
        }
        postValue(status)
    }

    // See https://developer.android.com/training/basics/network-ops/reading-network-state
    // we only use the default network in a first pass
    private fun getConnectivityManagerCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(logTag, "The default network is now: $network")
        }

        override fun onLost(network: Network) {
            Log.i(
                logTag,
                "The app no longer has a default network. The last default network was $network"
            )
            announceStatus(network, NetworkStatus.Unavailable)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {

            val status =
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    when {
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        -> NetworkStatus.Unmetered
                        !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
                        -> NetworkStatus.Roaming
                        !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        -> NetworkStatus.Metered
                        else -> {
                            Log.w(logTag, "Could not determine network status for $network")
                            Log.w(logTag, "Capabilities: $networkCapabilities")
                            NetworkStatus.Unknown
                        }
                    }
                } else {
                    NetworkStatus.Unavailable
                }

            // TODO re-implement a valid check to insure we can really access the internet
            announceStatus(network, status)
        }

//        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
//            Log.d(logTag, "The default network changed link properties: $linkProperties")
//        }
    }

    private fun getLegacyCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(logTag, "New network available: $network")
            // this is a known gotcha, we should not verify the status here
//                val networkCapability = connectivityManager.getNetworkCapabilities(network)
//                val hasNetworkConnection =
//                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                        ?: false
//                if (hasNetworkConnection) {
//                    determineInternetAccess(network, networkCapability)
//                }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            validNetworkConnections.remove(network)
            unMeteredConnections.remove(network)
            notRoamingConnections.remove(network)
            announceLegacyStatus()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                validNetworkConnections.add(network)
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    unMeteredConnections.add(network)
                } else {
                    unMeteredConnections.remove(network)
                }
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)) {
                    notRoamingConnections.add(network)
                } else {
                    notRoamingConnections.remove(network)
                }
            } else {
                validNetworkConnections.remove(network)
                unMeteredConnections.remove(network)
                notRoamingConnections.remove(network)
            }
            announceLegacyStatus()
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

//private object InternetAvailability {
//
//    // private val logTag = InternetAvailability::class.simpleName
//
//    fun check(network: Network): Boolean {
//        return try {
//            val socket = Socket()
//            socket.connect(InetSocketAddress("8.8.8.8", 53))
//            socket.close()
//
//            // TODO should we really check Google DNS?
////            val pydioUrl = ServerURLImpl.fromAddress("https://files.example.com")
////            try {
////                Log.e(logTag, "About to ping: $network")
////                pydioUrl.ping()
////                Log.e(logTag, "Ping succeed")
////                true
////            } catch (e: Exception) {
////                e.printStackTrace()
////                false
////            }
////            Log.e(logTag, "Checking internet connectivity for: ${network.toString()}")
//            true
//        } catch (e: Exception) {
//            e.printStackTrace()
//            false
//        }
//    }
//}


