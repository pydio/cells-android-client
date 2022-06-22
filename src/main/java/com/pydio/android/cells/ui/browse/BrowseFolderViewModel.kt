package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.CellsPreferences
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
    encodedStateID: String,
    private val nodeService: NodeService,
    prefs: CellsPreferences
) : ViewModel() {

    private val logTag = BrowseFolderViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val stateId: StateID = StateID.fromId(encodedStateID)
    val currentFolder = nodeService.getLiveNode(stateId)

    private var _children = nodeService.ls(stateId)
    val children: LiveData<List<RTreeNode>>
        get() = _children

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

    // Cache list order to only trigger order change when necessary
    private var _currentOrder = prefs.getString(
        AppNames.PREF_KEY_CURR_RECYCLER_ORDER,
        AppNames.DEFAULT_SORT_ENCODED
    )

    init {
        setLoading(true)
    }

    fun reQuery(newOrder: String) {
        if (_currentOrder != newOrder) {
            _currentOrder = newOrder
            _children = nodeService.ls(stateId)
        }
    }

    private fun watchFolder() = vmScope.launch {
        while (_isActive) {
            doPull()
            val nd = backOffTicker.getNextDelay()
            Log.d(logTag, "... Next delay: $nd - $stateId")
            delay(TimeUnit.SECONDS.toMillis(nd))
        }
        Log.i(logTag, "paused")
    }

    private suspend fun doPull() {
        Log.d(logTag, "About to pull for $stateId")
        val (changeNb, errMsg) = nodeService.pull(stateId)
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
        Log.i(logTag, "resumed")
        resetLiveChildren()
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

    fun orderHasChanged(newOrder: String): Boolean {
        return _currentOrder != newOrder
    }

    fun triggerRefresh() {
        setLoading(true)
        pause()
        currWatcher?.cancel()
        resume(true)
    }

    /** Force recreation of the liveData object, typically after sort order modification */
    private fun resetLiveChildren() {
        _children = nodeService.ls(stateId)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
