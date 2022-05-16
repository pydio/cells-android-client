package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.runtime.RNetworkInfo
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.utils.BackOffTicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Hold the session that is currently in foreground for browsing the cache
 * and the remote server.
 */
class ActiveSessionViewModel(
    private val accountService: AccountService,
    networkService: NetworkService,
    id: String = UUID.randomUUID().toString()
) : ViewModel() {

    private val logTag = "${ActiveSessionViewModel::class.simpleName}[$id]"
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Business objects
    val networkInfo: LiveData<RNetworkInfo> = networkService.getLiveStatus()
    private val isOnline = networkService.hasInternetAccess()

    private var _accountId: String? = null
    val accountId: String?
        get() = _accountId

    lateinit var sessionView: LiveData<RSessionView?>
    lateinit var workspaces: LiveData<List<RWorkspace>>

    fun isServerReachable():Boolean {
        if (sessionView.value == null){
            return false
        }
        return isOnline && sessionView.value?.authStatus == AppNames.AUTH_STATUS_CONNECTED
    }

    // Watcher states
    private var _isRunning = false
    private val backOffTicker = BackOffTicker()
    private var currWatcher: Job? = null

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun afterCreate(accountId: String?) {
        if (accountId != null) {
            _accountId = accountId
            Log.i(logTag, "Initializing active session for $accountId")
            sessionView = accountService.getLiveSession(accountId)
            workspaces = accountService.getLiveWorkspaces(accountId)
            setLoading(true)
        } else {
            // Awful tweak to insure late init objects have been initialized to avoid crash
            sessionView = accountService.getLiveSession("none")
            workspaces = accountService.getLiveWorkspaces("none")
            Log.e(logTag, "Initializing model with no account ID.")
        }
    }

    // TODO handle network status
    private fun watchSession() = vmScope.launch {
        while (_isRunning) {
            doPull()
            val nd = backOffTicker.getNextDelay()
            Log.d(logTag, "... $accountId - About to sleep $nd s.")
            delay(TimeUnit.SECONDS.toMillis(nd))
        }
        Log.i(logTag, "paused")
    }

    private suspend fun doPull() {
        if (accountId == null || sessionView.value == null) {
            Log.w(logTag, "No live session for $accountId ")
            return
        }
        withContext(Dispatchers.Main) {
            _errorMessage.value = null
        }
        val result = accountService.refreshWorkspaceList(accountId!!)
        withContext(Dispatchers.Main) {
            if (result.second != null) { // Non-Null response is an error message
                if (backOffTicker.getCurrentIndex() > 0) {
                    Log.e(logTag, "Could not list workspace...")
                    // We do not display the error message if first
                    _errorMessage.value = result.second
                }
                Log.i(logTag, "Pausing poll")
                pause()
            } else if (result.first > 0) {
                backOffTicker.resetIndex()
            }
            setLoading(false)
            if (result.second == null) { // Also reset error message
                _errorMessage.value = null
            }
        }
    }

    fun pause() {
        _isRunning = false
    }

    fun resume() {
        Log.d(logTag, "resuming...")
        backOffTicker.resetIndex()
        if (!_isRunning) {
            _isRunning = true
            currWatcher = watchSession()
        }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun forceRefresh() {
        setLoading(true)
        pause()
        currWatcher?.cancel()
        resume()
    }


    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "onCleared for $accountId")
        viewModelJob.cancel()
    }

}
