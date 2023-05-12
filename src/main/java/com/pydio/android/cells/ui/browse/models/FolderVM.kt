package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.core.AbstractBrowseVM
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.models.toTreeNodeItems
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
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
 * Simple ViewModel for the UI that only holds the current parent folder state ID
 * and a LiveData list of its children to be used while selecting a target folder.
 */
class FolderVM(
    private val stateID: StateID,
    prefs: PreferencesService,
    private val nodeService: NodeService
) : AbstractBrowseVM(prefs, nodeService) {

    private val logTag = "FolderVM"

    val childNodes: LiveData<List<RTreeNode>>
        get() = sortOrder.switchMap { currOrder ->
            if (Str.empty(stateID.slug)) {
                Log.e(logTag, "Listing workspaces in folderVM, this should never happen")
                nodeService.listLiveWorkspaces(stateID)
            } else {
                nodeService.sortedList(stateID, currOrder)
            }
        }

    // TODO this is the new approach, validate, factorize and remove LiveData
    private val orderFlow = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val children: StateFlow<List<TreeNodeItem>> = orderFlow.flatMapLatest { currPair ->
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
