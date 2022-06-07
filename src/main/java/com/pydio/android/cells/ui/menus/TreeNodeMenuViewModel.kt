package com.pydio.android.cells.ui.menus

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds a TreeNode for the various context menus.
 */
class TreeNodeMenuViewModel(
    val stateIDs: List<StateID>,
    val contextType: String,
    private val nodeService: NodeService,
) : ViewModel() {

    private val logTag = TreeNodeMenuViewModel::class.simpleName

    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val node = nodeService.getLiveNode(stateIDs[0])
    val nodes = nodeService.getLiveNodes(stateIDs)

    // init {
    //     Log.e(logTag, "init: list size: ${stateIDs.size}, first: ${stateIDs[0]}")
    //     vmScope.launch {
    //         withContext(Dispatchers.IO) {
    //             val tmpNode = stateIDs[0]?.let { nodeService.getNode(it) }
    //             Log.e(logTag, "init: tmp node: ${tmpNode?.getStateID()}")
    //             Log.e(
    //                 logTag,
    //                 "SELECT * FROM tree_nodes WHERE encoded_state = ${stateIDs[0]?.id} LIMIT 1"
    //             )
    //         }
    //     }
    // }

    private var _targetUri: Uri? = null
    val targetUri: Uri?
        get() = _targetUri

    fun prepareImport(uri: Uri) {
        vmScope.launch {
            _targetUri = uri
        }
    }
}
