package com.pydio.android.cells.utils

class NetworkStatusHelper {}


/*import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
}

class NetworkStatusHelper(context: Context) : LiveData<NetworkStatus>() {

    private val TAG = "NetworkStatusHelper"

    val validNetworkConnections: MutableMap<Int, Network> = HashMap()

    var connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    // Cache default value to avoid spamming the user at launch time
    private var wasOnline = true

    fun announceStatus() {
        Log.i(
            TAG,
            "Announcing status. Old: ${wasOnline}, new: ${validNetworkConnections.isNotEmpty()}"
        )
        Log.i(TAG, "Current network connection size: ${validNetworkConnections.size}")

        for ((hash, network) in validNetworkConnections){
            Log.i(TAG, "#${hash}")
        }

        if (validNetworkConnections.isNotEmpty() && !wasOnline) {
            wasOnline = true
            postValue(NetworkStatus.Available)
        } else if (validNetworkConnections.isEmpty() && wasOnline) {
            wasOnline = false
            postValue(NetworkStatus.Unavailable)
        }
    }

    private fun getConnectivityManagerCallback() =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "onAvailable: $network")

                super.onAvailable(network)
                val networkCapability = connectivityManager.getNetworkCapabilities(network)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (connectivityManager.activeNetwork == network){
                        Log.i(TAG, "Active network available")
                    } else {
                        Log.i(TAG, "Available network is not the active network")
                    }
                }

                val hasNetworkConnection =
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        ?: false

                val hasNetworkValidated =
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        ?: false

                val isMetered =
                    networkCapability?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        ?: false

                Log.i(TAG, "Available network has: internet: $hasNetworkConnection, " +
                        "validated: $hasNetworkValidated, is metered: $isMetered")

                if (hasNetworkConnection) {
                    determineInternetAccess(network)
                }
            }

            override fun onLost(network: Network) {
                Log.i(TAG, "onLost: $network")

                super.onLost(network)
                validNetworkConnections.remove(network.hashCode())
                announceStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                Log.i(TAG, "onCapabilitiesChanged: $network, type: ${getType(networkCapabilities)}")

                super.onCapabilitiesChanged(network, networkCapabilities)
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    determineInternetAccess(network)
                } else {
                    validNetworkConnections.remove(network.hashCode())
                    announceStatus()
                }
            }
        }

    private fun getType(networkCapabilities: NetworkCapabilities) : String {

        return when {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> "WIFI"
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) -> "NOT VPN"
            else -> "NONE"
        }


    }

    private fun determineInternetAccess(network: Network) {
        CoroutineScope(Dispatchers.IO).launch {
            val pingOK = Pinger.doPing()
            withContext(Dispatchers.Main) {
                if (pingOK) {
                    validNetworkConnections[network.hashCode()]= network
                    announceStatus()
                } else {
                    validNetworkConnections.remove(network.hashCode())
                    announceStatus()
                }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        connectivityManagerCallback = getConnectivityManagerCallback()
        val networkRequest = NetworkRequest
            .Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, connectivityManagerCallback)
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
    }

}

object Pinger {
    private const val TAG = "Pinger"

    fun doPing(): Boolean {
        return try {
            Log.i(TAG, "About to ping")
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53))
            socket.close()
            Log.i(TAG, "... Ping successful")
            true
        } catch (e: Exception) {
            Log.i(TAG, "... Ping failed")
            e.printStackTrace()
            false
        }
    }
}*/

