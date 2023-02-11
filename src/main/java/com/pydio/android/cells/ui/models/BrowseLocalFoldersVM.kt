package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val logTag = BrowseRemoteVM::class.simpleName

/**
 * Simple ViewModel for the UI that only holds the current parent folder state ID
 * and a LiveData list of its children to be used while selecting a target folder.
 */
class BrowseLocalFoldersVM(
    private val nodeService: NodeService
) : ViewModel() {

    private val _stateID = MutableStateFlow(StateID(/* serverUrl = */ Transport.UNDEFINED_URL))
    val stateID: StateFlow<StateID> = _stateID.asStateFlow()

    private lateinit var _children: LiveData<List<RTreeNode>>
    val childNodes: LiveData<List<RTreeNode>>
        get() = _children

    fun setState(stateID: StateID) {
        Log.e(logTag, "--- Updating current state to $stateID")
        _stateID.value = stateID

        _children = if (Str.empty(stateID.workspace)) {
            nodeService.listWorkspaces(stateID)
        } else {
            nodeService.ls(stateID)
        }
    }
}