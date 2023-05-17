package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.models.toTreeNodeItems
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel when browsing a Cells or P8 server. It holds the current parent folder state ID
 * and a Flow list of its children.
 */
class FolderVM(private val stateID: StateID) : AbstractCellsVM() {

//    private val logTag = "FolderVM"

    // Load the current parent for various labels and generic actions
    private val _rTreeNode = MutableStateFlow<RTreeNode?>(null)
    val treeNode: StateFlow<RTreeNode?> = _rTreeNode.asStateFlow()
    private val _rWorkspace = MutableStateFlow<RWorkspace?>(null)
    val workspace: StateFlow<RWorkspace?> = _rWorkspace.asStateFlow()

    // Observe current folder children
    @OptIn(ExperimentalCoroutinesApi::class)
    val children: StateFlow<List<TreeNodeItem>> = defaultOrderPair.flatMapLatest { currPair ->
        val rtNodes = if (Str.empty(stateID.slug)) {
            nodeService.listWorkspaces(stateID)
        } else {
            nodeService.sortedListFlow(stateID, currPair.first, currPair.second)
        }
        rtNodes.map { nodes -> toTreeNodeItems(nodeService, nodes) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf()
    )

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
