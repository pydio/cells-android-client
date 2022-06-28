package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
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
    private val prefs: CellsPreferences,
    private val networkService: NetworkService,
    private val accountService: AccountService,
    id: String = UUID.randomUUID().toString()
) : ViewModel() {

    private val logTag = "${ActiveSessionViewModel::class.simpleName}[${id.substring(24)}]"
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

//    // Business objects
//    private var _networkStatus: NetworkStatus = NetworkStatus.Available
//    private val _isConnected = MutableLiveData<Boolean>()
//    val isConnected: LiveData<Boolean>
//        get() = _isConnected

//    fun isOnline(): Boolean {
//        return networkService.liveInternetFlag?.value ?: false
//    }
//
//    fun isMetered(): Boolean {
//        return networkService.liveMeteredFlag?.value ?: false
//    }

    private var _accountId: String? = null
    val accountId: String?
        get() = _accountId

    lateinit var sessionView: LiveData<RSessionView?>
    lateinit var workspaces: LiveData<List<RWorkspace>>

    fun canListMeta(): Boolean {
        if (sessionView.value == null) {
            return false
        }
        val reachable = networkService.isConnected() &&
                sessionView.value?.authStatus == AppNames.AUTH_STATUS_CONNECTED
        if (!reachable) {
            Log.d(
                logTag, "Un-reachable. Connected: ${networkService.isConnected()} " +
                        ", auth status: ${sessionView.value?.authStatus}"
            )
        }
        return reachable
    }

    fun canDownloadFiles(): Boolean {
        if (sessionView.value == null) {
            return false
        }

        val dlFileOnMetered = prefs.getBoolean(AppNames.PREF_KEY_METERED_DL_FILES, false)
        return networkService.isConnected() &&
                sessionView.value?.authStatus == AppNames.AUTH_STATUS_CONNECTED &&
                (dlFileOnMetered || !networkService.isMetered())
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

//    init {
//        _isConnected.value = true
//        val liveNetwork = LiveNetwork(CellsApp.instance.applicationContext)
//        if (liveNetwork.value is NetworkStatus.Unavailable) {
//            setNetworkStatus(NetworkStatus.Unavailable)
//        }
//        vmScope.launch {
//            liveNetwork.asFlow().collect() {
//                it?.let {
//                    Log.e(logTag, "Setting new status: $it")
//                    setNetworkStatus(it)
//                }
//            }
//        }
//        Log.e(logTag, "Initial status: ${liveNetwork.value}")
//        Log.e(logTag, "Stored status: $_networkStatus")
//    }

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

//    private fun setNetworkStatus(status: NetworkStatus) {
//        Log.e(logTag, "############### Setting new status: $status")
//        this._networkStatus = status
//
//        _isConnected.value = _networkStatus !is NetworkStatus.Unavailable
//    }

    fun forceRefresh() {
        setLoading(true)
        pause()
        currWatcher?.cancel()
        resume()
    }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "onCleared for $accountId")
        vmJob.cancel()
    }

    private fun watchSession() = vmScope.launch {
        while (_isRunning) {
            val nd = if (networkService.isConnected()) {
                doPull()
                backOffTicker.getNextDelay()
            } else {
                setLoading(false)
                // backOffTicker.getCurrentDelay()
                2
            }
            Log.d(logTag, "... ${StateID.fromId(_accountId)} - About to sleep ${nd}s")
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
        val (changeNb, errMsg) = accountService.refreshWorkspaceList(accountId!!)
        withContext(Dispatchers.Main) {
            if (errMsg != null) { // Non-Null response is an error message
                if (backOffTicker.getCurrentIndex() > 0) {
                    Log.e(logTag, "could not list workspaces...")
                    // We do not display the error message if first
                    _errorMessage.value = errMsg
                }
                Log.i(logTag, "Pausing poll")
                pause()
            } else if (changeNb > 0) {
                backOffTicker.resetIndex()
            }
            setLoading(false)
            if (errMsg == null) { // Also reset error message
                _errorMessage.value = null
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
