package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private val logTag = BrowseRemoteVM::class.simpleName

/**
 * Simple ViewModel for the UI that only holds the current parent folder state ID
 * and a LiveData list of its children to be used while selecting a target folder.
 */
class BrowseLocalFoldersVM(
    private val nodeService: NodeService
) : ViewModel() {

    private var _stateID = MutableLiveData<StateID>()
    val stateID: LiveData<StateID>
        get() = _stateID

    private lateinit var _children: LiveData<List<RTreeNode>>
    val childNodes: LiveData<List<RTreeNode>>
        get() = _children

    init {
        _stateID.value = StateID(/* serverUrl = */ Transport.UNDEFINED_URL)
    }

    fun setState(stateID: StateID) {
        Log.d(logTag, "--- Updating current state to $stateID")
        _stateID.value = stateID

        _children = if (Str.empty(stateID.workspace)) {
            nodeService.listWorkspaces(stateID)
        } else {
            nodeService.ls(stateID)
        }
    }

//    @Deprecated("Rather use setState")
//    fun afterCreate(stateID: StateID) {
//        Log.d(logTag, "After Create, state ID: $stateID")
//        _children = nodeService.listChildFolders(stateID)
//    }
}
