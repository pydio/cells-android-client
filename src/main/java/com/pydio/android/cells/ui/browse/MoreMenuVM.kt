package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private val logTag = MoreMenuVM::class.simpleName

/**  Expose methods to manage a TreeNode */
class MoreMenuVM(
    private val nodeService: NodeService
) : ViewModel() {

    suspend fun getTreeNode(stateID: StateID) :RTreeNode? {
        return nodeService.getLocalNode(stateID)
    }

    // Fire and forget in viewModelScope
    fun createFolder(parentID: StateID, name: String) {
        viewModelScope.launch {
            val errMsg = nodeService.createFolder(parentID, name)
            if (Str.notEmpty(errMsg)){
                Log.e(logTag, "Could not create folder $name at $parentID: $errMsg")
            }
        }
    }
}
