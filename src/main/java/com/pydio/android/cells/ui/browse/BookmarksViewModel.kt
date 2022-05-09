package com.pydio.android.cells.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService

/**
 * Holds a live list of the cached bookmarks for the current session
 */
class BookmarksViewModel(private val nodeService: NodeService) : ViewModel() {

    private var _stateID: StateID? = null
    val stateID: StateID?
        get() = _stateID

    private lateinit var _bookmarks: LiveData<List<RTreeNode>>
    val bookmarks: LiveData<List<RTreeNode>>
        get() = _bookmarks

    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun afterCreate(stateID: StateID) {
        _stateID = stateID
        _bookmarks = nodeService.listBookmarks(stateID)
    }

    fun triggerRefresh() {
        // For the time being, we only launch the bookmark list refresh explicitly,
        // typically on resume from the corresponding fragment
        // TODO implement dynamic update
        stateID?.let {
            vmScope.launch {
                nodeService.refreshBookmarks(it)
            }
        }
    }
}
