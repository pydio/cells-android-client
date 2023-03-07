package com.pydio.android.cells.ui.aaLegacy.transferxml

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
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

/**
 * Holds a folder and all its children folders to choose a target destination
 * for uploads and copy/moves.
 */
class PickFolderViewModel(
    private val accountService: AccountService,
    private val nodeService: NodeService
) : ViewModel() {

    private val logTag = PickFolderViewModel::class.simpleName
    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Business objects
    private lateinit var _stateID: StateID
    val stateID: StateID
        get() = _stateID

    private lateinit var _children: LiveData<List<RTreeNode>>
    val children: LiveData<List<RTreeNode>>
        get() = _children

    // Technical local objects
    private val backOffTicker = BackOffTicker()
    private var currWatcher: Job? = null
    private var _isActive = false

    // UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    init {
        _isLoading.value = true
    }

    fun afterCreate(stateID: StateID) {
        Log.d(logTag, "After Create, state ID: $stateID")

        // Cannot work: first time we pass here, the late-init prop is not yet initialized
        //        if (stateID != null && stateID.equals(_stateID)){
        //            // Nothing to do
        //            Log.d(logTag, "After create, state unchanged: $stateID")
        //            return
        //        }

        _stateID = stateID
        _children = nodeService.listChildFolders(stateID)
    }

    private fun watchFolder() = vmScope.launch {
        while (_isActive) {
            doPull()
            val nd = backOffTicker.getNextDelay()
            delay(TimeUnit.SECONDS.toMillis(nd))
            Log.d(logTag, "... Watching folders at ${_stateID.toString()}, next delay: ${nd}s")
        }
    }

    private suspend fun doPull() {
        val result = if (Str.empty(stateID.file)) {
            accountService.refreshWorkspaceList(stateID.account())
        } else {
            nodeService.pull(stateID)
        }
        withContext(Dispatchers.Main) {
            if (Str.notEmpty(result.second)) {
                _errorMessage.value = result.second
                pause()
            }
            if (result.first > 0) { // At least one change => reset backoff ticker
                backOffTicker.resetIndex()
            }
            _isLoading.value = false
        }
//        if (Str.empty(stateID.file)) {
//            val result = accountService.refreshWorkspaceList(stateID.accountId)
//            withContext(Dispatchers.Main) {
//                if (Str.notEmpty(result.second)) {
//                    _errorMessage.value = result.second
//                    pause()
//                }
//                if (result.first > 0) { // At least one change => reset backoff ticker
//                    backOffTicker.resetIndex()
//                }
//                _isLoading.value = false
//            }
//        } else {
//            val result = nodeService.pull(stateID)
//            withContext(Dispatchers.Main) {
//                if (result.second != null) {
//                    _errorMessage.value = result.second
//                    pause()
//                } else if (result.first > 0) {
//                    backOffTicker.resetIndex()
//                }
//                _isLoading.value = false
//            }
//        }
    }

    fun resume() {
        if (!_isActive) {
            _isActive = true
            currWatcher = watchFolder()
        }
        backOffTicker.resetIndex()
    }

    fun pause() {
        if (_isActive) {
            Log.d(logTag, "... real pause called by")
            Thread.dumpStack()
        }
        _isActive = false
    }

    fun forceRefresh() {
        _isLoading.value = true
        pause()
        currWatcher?.cancel()
        resume()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
