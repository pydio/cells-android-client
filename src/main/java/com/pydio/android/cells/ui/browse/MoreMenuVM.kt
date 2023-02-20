package com.pydio.android.cells.ui.browse

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTreeNode
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

private val logTag = MoreMenuVM::class.simpleName

/**  Expose methods to manage a TreeNode */
class MoreMenuVM(
    private val nodeService: NodeService,
    private val fileService: FileService,
    private val transferService: TransferService,
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

    fun importPhoto() {
        _targetForPhoto?.let {
            viewModelScope.launch {
                transferService.enqueueUpload(it.first, it.second)
            }
        }
    }

    fun cancelPhoto() {
        _targetForPhoto = null
    }

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

    fun restoreFromtrash(stateID: StateID) {
        viewModelScope.launch {
            nodeService.restoreNode(stateID)
        }
    }

    suspend fun getShareLink(stateID: StateID): String? = withContext(Dispatchers.IO) {
        nodeService.getLocalNode(stateID)?.getShareAddress()
    }


//    fun toggleBookmark(stateID: StateID, newState: Boolean) {
//        viewModelScope.launch {
//            nodeService.toggleBookmark(stateID, newState)
//        }
//    }
//
//    ACTION_TOGGLE_SHARED -> {
//        // TODO ask confirmation
//        nodeService.toggleShared(node)?.let {
//            // If we created a link we get it as result and put it in the clipboard directly
//            val clipboard =
//                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
//            if (clipboard != null) {
//                val clip = ClipData.newPlainText(node.name, it)
//                clipboard.setPrimaryClip(clip)
//                showMessage(
//                    requireContext(),
//                    resources.getString(R.string.link_copied_to_clip)
//                )
//            }
//        }
//        doDismiss()
//    }
//    ACTION_PUBLIC_LINK_COPY -> {
//        val clipboard =
//            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
//        val link = node.getShareAddress()
//        if (clipboard != null && link != null) {
//            val clip = ClipData.newPlainText(node.name, link)
//            clipboard.setPrimaryClip(clip)
//            showMessage(
//                requireContext(),
//                resources.getString(R.string.link_copied_to_clip)
//            )
//        } else { // Should never happen.
//            showMessage(
//                requireContext(),
//                resources.getString(R.string.link_copy_failed)
//            )
//        }
//        doDismiss()
//    }
//    ACTION_DISPLAY_AS_QRCODE -> {
//        displayAsQRCode(requireContext(), node)
//        doDismiss()
//    }
//    ACTION_PUBLIC_LINK_SHARE -> {
//        node.getShareAddress()?.let { link ->
//            val shareIntent = Intent(Intent.ACTION_SEND)
//                .setType("text/plain")
//                .putExtra(Intent.EXTRA_TEXT, link)
//            startActivity(shareIntent)
//        }
//        doDismiss()
//    }


}
