package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

private val logTag = BrowseHostVM::class.simpleName

/**  Expose methods to simplify navigation while browsing*/
class BrowseHostVM(
    private val nodeService: NodeService
) : ViewModel() {
    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }
}
