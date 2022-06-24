package com.pydio.android.cells.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Holds a live list of the cached bookmarks for the current session
 */
class BookmarksViewModel(private val nodeService: NodeService) : ViewModel() {

    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    private var _stateID: StateID? = null
    val stateID: StateID?
        get() = _stateID

    private lateinit var _bookmarks: LiveData<List<RTreeNode>>
    val bookmarks: LiveData<List<RTreeNode>>
        get() = _bookmarks

    // Cache list order to only trigger order change when necessary
    private lateinit var _currentOrder: String

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

    fun afterCreate(stateID: StateID, currentOrder: String) {
        _stateID = stateID
        _bookmarks = nodeService.listBookmarks(stateID)
        _currentOrder = currentOrder
    }

    fun orderHasChanged(newOrder: String): Boolean {
        return _currentOrder != newOrder
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun triggerRefresh() {
        // For the time being, we do not poll the server for bookmarks:
        // the API is only queried:
        //   - during fragment onResume step
        //   - when explicitly requested by the end user (swipe refresh).
        stateID?.let {
            vmScope.launch {
                nodeService.refreshBookmarks(it)
                setLoading(false)
            }
        } ?: run {
            setLoading(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

}
