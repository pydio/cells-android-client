package com.pydio.android.cells.ui.browsexml

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Holds a folder and all its children */
class BrowseFolderViewModel(
    prefs: CellsPreferences,
    private val networkService: NetworkService,
    private val nodeService: NodeService,
    encodedStateID: String
) : ViewModel() {

    private val logTag = BrowseFolderViewModel::class.simpleName
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    val stateId: StateID = StateID.fromId(encodedStateID)
    val currentFolder = nodeService.getLiveNode(stateId)

    val liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())

    // Enable LiveData update when the sort order changes
    private val liveSortValue: MutableLiveData<String> = liveSharedPreferences.getString(
        AppKeys.CURR_RECYCLER_ORDER,
        AppNames.DEFAULT_SORT_ENCODED
    )

    fun getChildrenWithLiveSort(): LiveData<List<RTreeNode>> {
        return Transformations.switchMap(liveSortValue) { nodeService.ls(stateId) } // Log.e(logTag, "sort order has changed: $encodedSort")
    }

    private val backOffTicker = BackOffTicker()
    private var _isActive = false
    private var currWatcher: Job? = null

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        setLoading(true)
    }

    private fun watchFolder() = vmScope.launch {
        while (_isActive) {
            val nd = if (networkService.isConnected()) {
                doPull()
                backOffTicker.getNextDelay()
            } else {
                setLoading(false)
                // backOffTicker.getCurrentDelay()
                2
            }
            Log.d(logTag, "... $stateId - About to sleep ${nd}s")
            delay(TimeUnit.SECONDS.toMillis(nd))
        }
        Log.i(logTag, "paused")
    }

    private suspend fun doPull() {

        Log.d(logTag, "### Launching remote pull for $stateId")
        val (changeNb, errMsg) = nodeService.pull(stateId)
        // Log.d(logTag, "... After pull with $changeNb changes, msg: $errMsg")

        withContext(Dispatchers.Main) {
            if (Str.notEmpty(errMsg)) {
                if (backOffTicker.getCurrentIndex() > 0) {
                    // Not optimal, we should rather check the current session status
                    // before launching the poll
                    // We do not display the error message if first
                    _errorMessage.value = errMsg
                }
                pause()
            } else if (changeNb > 0) {
                backOffTicker.resetIndex()
            }
            setLoading(false)
        }
    }

    fun resume(resetBackOffTicker: Boolean) {
        Log.d(logTag, "Resumed - reset ticker: $resetBackOffTicker")
        if (!_isActive) {
            _isActive = true
            currWatcher = watchFolder()
        }
        if (resetBackOffTicker) {
            backOffTicker.resetIndex()
        }
    }

    fun pause() {
        _isActive = false
    }

    fun triggerRefresh() {
        setLoading(true)
        pause()
        currWatcher?.cancel()
        resume(true)
    }

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
