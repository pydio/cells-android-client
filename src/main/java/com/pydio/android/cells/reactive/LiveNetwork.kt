package com.pydio.android.cells.reactive
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.util.Log
//import androidx.lifecycle.LiveData
//import com.pydio.android.cells.services.CellsNetworkCallback
//import com.pydio.android.cells.services.NetworkStatus
//private const val logTag = "LiveNetwork"
//
///**
// * Relies on the device connectivity manager to expose current Network status as liveData
// * to ease use in the UI layers.
// */
//class LiveNetwork(context: Context) : LiveData<NetworkStatus>() {
//
//
//    private var connectivityManager: ConnectivityManager =
//        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback
//
//    override fun onActive() {
//        Log.i(logTag, "onActive()")
//        super.onActive()
//        connectivityManagerCallback = getConnectivityManagerCallback()
//        connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback)
//    }
//
//    override fun onInactive() {
//        Log.i(logTag, "onInactive()")
//        super.onInactive()
//        connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
//    }
//
//    // See https://developer.android.com/training/basics/network-ops/reading-network-state
//    // we only use the default network in a first pass
//    private fun getConnectivityManagerCallback() = CellsNetworkCallback { postValue(it) }
//}
