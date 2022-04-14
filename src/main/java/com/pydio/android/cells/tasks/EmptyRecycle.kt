package com.pydio.android.cells.tasks

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showLongMessage

fun emptyRecycle(
    context: Context,
    node: RTreeNode,
    nodeService: NodeService,
): Boolean {

    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.confirm_permanent_deletion_title)
        .setIcon(R.drawable.ic_baseline_delete_24)
        .setMessage(
            context.resources.getString(
                R.string.confirm_empty_recycle_message,
                node.workspace
            )
        )
        .setPositiveButton(R.string.button_confirm) { _, _ ->
            doEmptyRecycle(context, node, nodeService)
        }
        .setNegativeButton(R.string.button_cancel, null)
        .show()
    return true
}

private fun doEmptyRecycle(context: Context, node: RTreeNode, nodeService: NodeService) {
    CellsApp.instance.appScope.launch {
        nodeService.emptyRecycle(StateID.fromId(node.encodedState))
            ?.let {
                withContext(Dispatchers.Main) {
                    showLongMessage(context, it)
                }
            }
    }
}
