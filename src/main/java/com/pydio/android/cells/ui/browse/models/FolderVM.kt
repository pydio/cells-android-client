package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Simple ViewModel for the UI that only holds the current parent folder state ID
 * and a LiveData list of its children to be used while selecting a target folder.
 */
class FolderVM(
    private val nodeService: NodeService
) : ViewModel() {

    private val logTag = FolderVM::class.simpleName

//    private val _stateID = MutableStateFlow(Transport.UNDEFINED_STATE_ID)
//    val stateID: StateFlow<StateID> = _stateID.asStateFlow()

    private var _stateID: StateID = Transport.UNDEFINED_STATE_ID
    val stateID = _stateID

    private val _rTreeNode = MutableStateFlow<RTreeNode?>(null)
    val treeNode: StateFlow<RTreeNode?> = _rTreeNode.asStateFlow()
    private val _rWorkspace = MutableStateFlow<RWorkspace?>(null)
    val workspace: StateFlow<RWorkspace?> = _rWorkspace.asStateFlow()

//    private lateinit var _rTreeNode: MutableLiveData<RTreeNode>
//    val rTreeNode: LiveData<RTreeNode>
//        get() = _rTreeNode

//    private lateinit var _rWorkspace: MutableLiveData<RWorkspace>
//    val rWorkspace: LiveData<RWorkspace>
//        get() = _rWorkspace

    private lateinit var _children: LiveData<List<RTreeNode>>
    val childNodes: LiveData<List<RTreeNode>>
        get() = _children

    fun setState(stateID: StateID) {
        Log.i(logTag, "--- Updating current state to $stateID")
        _stateID = stateID

        _children = if (Str.empty(stateID.workspace)) {
            nodeService.listWorkspaces(stateID)
        } else {
            nodeService.ls(stateID)
        }

        viewModelScope.launch {
            nodeService.getNode(stateID)?.let {
                _rTreeNode.value = it
                if (it.isWorkspaceRoot()) {
                    _rWorkspace.value = nodeService.getWorkspace(stateID)
                }

            }
        }
    }

    fun isInRecycle(): Boolean {
        treeNode.value?.let {
            val inRecycle = it.isInRecycle() || it.isRecycle()
            Log.i(logTag, "--- computing recycle status for ${it.getStateID()}: $inRecycle")
            return inRecycle
        } ?: run {
            Log.i(logTag, "--- cannot check recycle status for ${stateID} no RTreeNode")
        }

        return false
    }
}
