package com.pydio.android.cells.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Hold the session that is currently in foreground for browsing the cache
 * and the remote server.
 */
class ActiveSessionViewModel(
    private val prefs: PreferencesService,
    private val jobService: JobService,
    private val networkService: NetworkService,
    private val accountService: AccountService,
    id: String = UUID.randomUUID().toString(),
) : ViewModel() {

    private val logTag = "ActiveSessionViewModel[${id.substring(24)}]"
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    // private val livePrefs = LiveSharedPreferences(prefs.get())

    private var _accountId: String? = null
    val accountId: String?
        get() = _accountId

    lateinit var sessionView: LiveData<RSessionView?>
    lateinit var workspaces: LiveData<List<RWorkspace>>

    fun canListMeta(): Boolean {
        if (sessionView.value == null) {
            return false
        }
        val reachable =
            networkService.isConnected() && sessionView.value?.authStatus == AppNames.AUTH_STATUS_CONNECTED
        if (!reachable) {
            Log.d(
                logTag,
                "Un-reachable. Connected: ${networkService.isConnected()} " + ", auth status: ${sessionView.value?.authStatus}"
            )
        }
        return reachable
    }

    suspend fun canDownloadFiles(): Boolean {
        if (sessionView.value == null) {
            return false
        }

        val dlFileOnMetered = prefs.fetchPreferences().meteredNetwork.askBeforeDL
        return networkService.isConnected() && sessionView.value?.authStatus == AppNames.AUTH_STATUS_CONNECTED && (dlFileOnMetered || !networkService.isMetered())
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

    // FIXME
    // private var oldOfflineFrequency = prefs.getString(AppKeys.SYNC_FREQ, AppNames.SYNC_FREQ_WEEK)

    init {
        configureWorkers()
    }

    fun afterCreate(accountId: String?) {
        if (accountId != null) {
            _accountId = accountId
            Log.i(logTag, "Initializing active session for $accountId")
            sessionView = accountService.getLiveSession(accountId)
            workspaces = accountService.getLiveWorkspaces(accountId)
            setLoading(true)

            // // FIXME remove this as only been added for debug purposes
            // vmScope.launch {
            //     transferService.createJobs(StateID.fromId(accountId))
            // }
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
        backOffTicker.resetIndex()
        if (!_isRunning) {
            _isRunning = true
            currWatcher = watchSession()
        }
        _errorMessage.value = null
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
        vmJob.cancel()
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    // Initialisation of workers is done at App start, we only register observer for change here.
    private fun configureWorkers() {
        vmScope.launch {
            // FIXME
//            livePrefs.getString(AppKeys.SYNC_FREQ, AppNames.SYNC_FREQ_WEEK).asFlow().collect {
//                    // Log.e(logTag, "### Live share pref event: $it")
//                    it?.let {
//                        if (it != oldOfflineFrequency) {
//                            resetWorker()
//                        }
//                    }
//                }
//            // TODO also observe offline constraints settings
        }
    }

//    private suspend fun resetWorker() {
//
//        // Debug info
//        val it = prefs.getString(AppKeys.SYNC_FREQ, AppNames.SYNC_FREQ_WEEK)
//        val prefix = "### Cancel and restart offline worker with [$it] frequency "
//        try {
//            oldOfflineFrequency = it
//            jobService.i(logTag, prefix, "Live Pref Observer")
//        } catch (e: Exception) {
//            Log.e(logTag, "$prefix, could not log to user table: ${e.message}")
//        }
//
//        // Effective reset.
//        // TODO make it more clever to prevent systematic launch of the worker each time the preferences change
//        val workManager = WorkManager.getInstance(CellsApp.instance)
//        workManager.cancelUniqueWork(OfflineSync.WORK_NAME)
//        workManager.enqueueUniquePeriodicWork(
//            OfflineSync.WORK_NAME,
//            ExistingPeriodicWorkPolicy.REPLACE,
//            OfflineSync.buildWorkRequest(prefs),
//        )
//    }

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
        val (changeNb, errMsg) = accountService.refreshWorkspaceList(StateID.fromId(accountId))
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
}
