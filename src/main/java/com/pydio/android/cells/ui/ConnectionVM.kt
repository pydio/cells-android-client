package com.pydio.android.cells.ui


import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.util.*

/**
 * Hold the session that is currently in foreground for browsing the cache
 * and the remote server.
 */
class ConnectionVM(
    private val networkService: NetworkService,
    id: String = UUID.randomUUID().toString(),
) : ViewModel() {

    private val logTag = "${ConnectionVM::class.simpleName}[${id.substring(24)}]"
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    val liveNetwork = networkService.networkType

    fun isConnected(networkType: String?): Boolean =
        networkType?.let { networkService.isConnected(it) } ?: true

    fun isLimited(networkType: String?): Boolean =
        networkType?.let { networkService.isLimited(it) } ?: false


    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
        Log.e(logTag, "Cleared Connection view model")
    }
}
