package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.BackOffTicker
import com.pydio.cells.transport.StateID
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

    init {
        setLoading(true)
    }

/*
    fun afterCreate(stateId: StateID){
        _stateId = stateId
        _currentFolder = nodeService.getLiveNode(stateId)
        _children = nodeService.ls(stateId)
    }
*/

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
        val result = nodeService.pull(stateId)
        withContext(Dispatchers.Main) {
            if (result.second != null) {
                if (backOffTicker.getCurrentIndex() > 0) {
                    // Not optimal, we should rather check the current session status
                    // before launching the poll
                    // We do not display the error message if first
                    _errorMessage.value = result.second
                }
                pause()
            } else if (result.first > 0) {
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

    fun forceRefresh() {
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

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
