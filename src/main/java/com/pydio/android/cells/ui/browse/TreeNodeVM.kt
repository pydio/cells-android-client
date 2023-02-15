package com.pydio.android.cells.ui.browse

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

private val logTag = TreeNodeVM::class.simpleName

/**  Expose methods to manage a TreeNode */
class TreeNodeVM(
    private val nodeService: NodeService
) : ViewModel() {

    suspend fun getTreeNode(stateID: StateID) :RTreeNode? {
        return nodeService.getLocalNode(stateID)
    }
}
