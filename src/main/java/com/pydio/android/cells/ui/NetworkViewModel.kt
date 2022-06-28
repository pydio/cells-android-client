package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.reactive.LiveNetwork
import com.pydio.android.cells.reactive.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

/** Shortcut to ease watch of the network status */
class NetworkViewModel(
    id: String = UUID.randomUUID().toString()
) : ViewModel() {

    private val logTag = "${NetworkViewModel::class.simpleName}[${id.substring(24)}]"
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    // Business objects
    private var _networkStatus: NetworkStatus = NetworkStatus.Available
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean>
        get() = _isConnected
    private val _isMetered = MutableLiveData<Boolean>()
    val isMetered: LiveData<Boolean>
        get() = _isMetered

    // Manage UI
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        _isConnected.value = true
        _isMetered.value = false
        val liveNetwork = LiveNetwork(CellsApp.instance.applicationContext)
        if (liveNetwork.value is NetworkStatus.Unavailable) {
            setNetworkStatus(NetworkStatus.Unavailable)
        } else if (liveNetwork.value is NetworkStatus.Metered) {
            setNetworkStatus(NetworkStatus.Metered)
        }
        vmScope.launch {
            liveNetwork.asFlow().collect() {
                it?.let {
                    Log.e(logTag, "Setting new status: $it")
                    setNetworkStatus(it)
                }
            }
        }
        Log.e(logTag, "Initial status: ${liveNetwork.value}")
        Log.e(logTag, "Stored status: $_networkStatus")
    }

    private fun setNetworkStatus(status: NetworkStatus) {
        Log.e(logTag, "############### Setting new status: $status")
        this._networkStatus = status

        _isConnected.value = _networkStatus !is NetworkStatus.Unavailable
        _isMetered.value = _networkStatus !is NetworkStatus.Metered
    }

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }
}
