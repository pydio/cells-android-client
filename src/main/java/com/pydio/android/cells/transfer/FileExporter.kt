package com.pydio.android.cells.transfer

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pydio.cells.transport.StateID
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService

class FileExporter(
    private val registry: ActivityResultRegistry,
    private val stateID: StateID?,
    private val caller: String,
    private val callingFragment: BottomSheetDialogFragment,
) : DefaultLifecycleObserver, KoinComponent {

    private val logTag = FileExporter::class.simpleName
    private val createDocumentKey = AppNames.KEY_PREFIX_ + "create.files"
    private val createMediaKey = AppNames.KEY_PREFIX_ + "create.media"

    lateinit var createDocument: ActivityResultLauncher<String>

    private val nodeService: NodeService by inject()
    // private val transferService: TransferService by inject()


    override fun onCreate(owner: LifecycleOwner) {
        createDocument = registry.register(
            createDocumentKey,
            owner,
            ActivityResultContracts.CreateDocument()
        )
        { uri ->
            stateID?.let {
                nodeService.enqueueDownload(it, uri!!)
                Log.i(caller, "Received download intent to parent path at $uri for $stateID")
                callingFragment.dismiss()
                // nodeService.enqueueUpload(it, uri)
            } ?: run {
                Log.w(caller, "Received file at $uri with **no** parent stateID")
            }

        }
    }

    fun pickTargetLocation(rTreeNode: RTreeNode) {
        Log.w(caller, "Pick target location for mime: ${rTreeNode.name} ($stateID)")
        createDocument.launch(rTreeNode.name)
    }
}
