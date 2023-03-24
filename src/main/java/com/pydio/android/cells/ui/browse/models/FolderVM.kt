package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
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
    private val stateID: StateID,
    prefs: PreferencesService,
    private val nodeService: NodeService,
    transferService: TransferService
) : AbstractBrowseVM(prefs, nodeService, transferService) {

    private val logTag = "FolderVM"

    val childNodes: LiveData<List<RTreeNode>>
        get() = sortOrder.switchMap { currOrder ->
            if (Str.empty(stateID.workspace)) {
                Log.e(logTag, "Listing workspaces in folderVM, this should never happen")
                nodeService.listWorkspaces(stateID)
            } else {
                nodeService.sortedList(stateID, currOrder)
            }
        }

    private val _rTreeNode = MutableStateFlow<RTreeNode?>(null)
    val treeNode: StateFlow<RTreeNode?> = _rTreeNode.asStateFlow()

    private val _rWorkspace = MutableStateFlow<RWorkspace?>(null)
    val workspace: StateFlow<RWorkspace?> = _rWorkspace.asStateFlow()

    init {
        viewModelScope.launch {
            nodeService.getNode(stateID)?.let {
                _rTreeNode.value = it
                if (it.isWorkspaceRoot()) {
                    _rWorkspace.value = nodeService.getWorkspace(stateID)
                }
            }
        }
    }
}
