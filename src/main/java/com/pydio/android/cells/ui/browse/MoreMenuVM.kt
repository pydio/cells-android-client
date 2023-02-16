package com.pydio.android.cells.ui.browse

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private val logTag = MoreMenuVM::class.simpleName

/**  Expose methods to manage a TreeNode */
class MoreMenuVM(
    private val nodeService: NodeService
) : ViewModel() {

    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getLocalNode(stateID)
    }

    // Fire and forget in viewModelScope
    fun createFolder(parentID: StateID, name: String) {
        viewModelScope.launch {
            val errMsg = nodeService.createFolder(parentID, name)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not create folder $name at $parentID: $errMsg")
            }
        }
    }

    fun renameNode(srcID: StateID, name: String) {
        viewModelScope.launch {
            val errMsg = nodeService.rename(srcID, name)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not rename $srcID to $name: $errMsg")
            }
        }
    }

    fun deleteNode(stateID: StateID) {
        viewModelScope.launch {
            val errMsg = nodeService.delete(stateID)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not delete node at $stateID: $errMsg")
            }
        }
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            nodeService.saveToSharedStorage(stateID, uri)
            // FIXME handle exception
        }
    }

    fun copyTo(stateID: StateID, targetParentID: StateID) {
        // TODO better handling of scope and error messages
        CellsApp.instance.appScope.launch {
            // TODO what do we store/show?
            //   - source files
            //   - target files
            //   - processing
            val errMsg = nodeService.copy(listOf(stateID), targetParentID)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not move node $stateID to $targetParentID")
                Log.e(logTag, "Cause: $errMsg")
            }
        }
    }

    fun moveTo(stateID: StateID, targetParentID: StateID) {
        // TODO better handling of scope and error messages
        CellsApp.instance.appScope.launch {
            // TODO what do we store/show?
            //   - source files
            //   - target files
            //   - processing
            val errMsg = nodeService.move(listOf(stateID), targetParentID)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not move node $stateID to $targetParentID")
                Log.e(logTag, "Cause: $errMsg")
            }
        }
    }


}
