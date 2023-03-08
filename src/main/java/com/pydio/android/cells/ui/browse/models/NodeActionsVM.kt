package com.pydio.android.cells.ui.browse.models

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.utils.DEFAULT_FILE_PROVIDER_ID
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


/**  Centralize methods to manage a TreeNode */
class NodeActionsVM(
    private val nodeService: NodeService,
    private val fileService: FileService,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = "NodeActionsVM"

    // Fire and forget in viewModelScope
    fun createFolder(parentID: StateID, name: String) {
        viewModelScope.launch {
            val errMsg = nodeService.createFolder(parentID, name)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not create folder $name at $parentID: $errMsg")
            }
        }
    }

    fun rename(srcID: StateID, name: String) {
        viewModelScope.launch {
            val errMsg = nodeService.rename(srcID, name)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not rename $srcID to $name: $errMsg")
            }
        }
    }

    fun delete(stateID: StateID) {
        viewModelScope.launch {
            val errMsg = nodeService.delete(stateID)
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "Could not delete node at $stateID: $errMsg")
            }
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

    fun emptyRecycle(stateID: StateID) {
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

    fun importFiles(stateID: StateID, uris: List<Uri>) {
        viewModelScope.launch {
            for (uri in uris) {
                transferService.enqueueUpload(stateID, uri)
            }   // FIXME handle exception
        }
    }

    private var _targetForPhoto: Pair<StateID, Uri>? = null

    suspend fun preparePhoto(context: Context, parentID: StateID): Uri? =
        withContext(Dispatchers.IO) {
            val photoFile: File? = try {
                fileService.createImageFile(parentID)
            } catch (ex: IOException) {
                Log.e(logTag, "Cannot create picture file")
                ex.printStackTrace()
                // Error occurred while creating the File
                null
            }

            photoFile?.also { // Continue only if the File was successfully created
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    DEFAULT_FILE_PROVIDER_ID,
                    it
                )
                withContext(Dispatchers.Main) { // We keep the state in the current VM
                    _targetForPhoto = Pair(parentID, uri)
                }
                return@withContext uri
            }
            return@withContext null
        }

    fun uploadPhoto() {
        _targetForPhoto?.let {
            viewModelScope.launch {
                transferService.enqueueUpload(it.first, it.second)
            }
        }
    }

    fun cancelPhoto() {
        _targetForPhoto = null
    }

    fun toggleBookmark(stateID: StateID, newState: Boolean) {
        viewModelScope.launch {
            nodeService.toggleBookmark(stateID, newState)
        }
    }

    fun toggleOffline(stateID: StateID, newState: Boolean) {
        viewModelScope.launch {
            nodeService.toggleOffline(stateID, newState)
        }
    }

    fun createShare(stateID: StateID) {
        viewModelScope.launch {
            nodeService.createShare(stateID)
        }
    }

    fun removeShare(stateID: StateID) {
        viewModelScope.launch {
            nodeService.removeShare(stateID)
        }
    }

    fun restoreFromTrash(stateID: StateID) {
        viewModelScope.launch {
            nodeService.restoreNode(stateID)
        }
    }

    suspend fun getShareLink(stateID: StateID): String? = withContext(Dispatchers.IO) {
        nodeService.getNode(stateID)?.getShareAddress()
    }
}
