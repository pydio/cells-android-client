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

fun moveToRecycle(
    context: Context,
    node: RTreeNode,
    nodeService: NodeService,
): Boolean {

    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.confirm_move_to_recycle_title)
        .setIcon(R.drawable.ic_baseline_delete_24)
        .setMessage(context.resources.getString(R.string.confirm_move_to_recycle_desc, node.name))
        .setPositiveButton(R.string.button_confirm) { _, _ ->
            doMoveToRecycle(context, node.encodedState, nodeService)
        }
        .setNegativeButton(R.string.button_cancel, null)
        .show()
    return true
}

fun moveNodesToRecycle(
    context: Context,
    states: List<StateID>,
    nodeService: NodeService,
): Boolean {

    val msg = if (states.size == 1) {
        context.resources.getQuantityString(R.plurals.confirm_multi_move_to_recycle_desc, 1)
    } else {
        String.format(
            context.resources.getQuantityString(
                R.plurals.confirm_multi_move_to_recycle_desc,
                states.size
            ), states.size
        )
    }

    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.confirm_move_to_recycle_title)
        .setIcon(R.drawable.ic_baseline_delete_24)
        .setMessage(msg)
        .setPositiveButton(R.string.button_confirm) { _, _ ->

            for (stateId in states) {
                doMoveToRecycle(context, stateId.id, nodeService)
            }

        }
        .setNegativeButton(R.string.button_cancel, null)
        .show()
    return true
}

private fun doMoveToRecycle(context: Context, encodedState: String, nodeService: NodeService) {
    CellsApp.instance.appScope.launch {
        nodeService.delete(StateID.fromId(encodedState))
            ?.let {
                withContext(Dispatchers.Main) {
                    showLongMessage(context, it)
                }
            }
    }
}
