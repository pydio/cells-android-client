package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.core.ListLayout
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
    private val prefs: CellsPreferences,
    private val nodeService: NodeService
) : ViewModel() {

    private val logTag = FolderVM::class.simpleName

    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())

    private val _layout = MutableStateFlow(ListLayout.LIST)
    val layout: StateFlow<ListLayout> = _layout.asStateFlow()

    private val sortOrder =
        liveSharedPreferences.getString(AppKeys.CURR_RECYCLER_ORDER, AppNames.DEFAULT_SORT_BY)

    val childNodes: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(
            sortOrder
        ) { currOrder ->
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

            // Smelly code we parse String as enum and go from LiveData to flow
            liveSharedPreferences.getString(
                AppKeys.CURR_RECYCLER_LAYOUT,
                AppNames.RECYCLER_LAYOUT_LIST
            ).asFlow().collect {
                if (it != _layout.value.name) {
                    val newValue = try {
                        ListLayout.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        ListLayout.LIST
                    }
                    _layout.value = newValue
                }
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
    }

    fun setListLayout(listLayout: ListLayout) {
        prefs.setString(AppKeys.CURR_RECYCLER_LAYOUT, listLayout.name)
    }
}
