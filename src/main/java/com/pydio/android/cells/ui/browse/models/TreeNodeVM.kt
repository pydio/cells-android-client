package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

/**  Simply provides access to the DB to retrieve basic single objects from the DB */
class TreeNodeVM(
    stateID: StateID,
    private val nodeService: NodeService,
) : ViewModel() {

    private val logTag = "NodeActionsVM"

    // TODO load the Nodes at init

    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    suspend fun getWS(stateID: StateID): RWorkspace? {
        return nodeService.getWorkspace(stateID)
    }

    init {
        Log.i(logTag, "Created TreeNodeVM")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(logTag, "Cleared TreeNodeVM")
    }
}
