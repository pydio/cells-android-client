package com.pydio.android.cells.tasks

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showLongMessage

fun rename(
    context: Context,
    node: RTreeNode,
    nodeService: NodeService,
): Boolean {

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.rename_dialog_title)
        .setIcon(R.drawable.ic_baseline_drive_file_rename_outline_24)
        .setView(R.layout.dialog_edit_text)
        .setMessage(context.resources.getString(R.string.rename_dialog_message, node.name))
        .setPositiveButton(R.string.rename_dialog_confirm_button) { dialog, _ ->
            (dialog as AlertDialog).findViewById<TextView>(android.R.id.text1)?.let { input ->
                if (input.text.isNullOrEmpty()) {
                    showLongMessage(context, "Please enter a valid not-empty name")
                } else {
                    doRename(context, node, input.text, nodeService)
                }
            }

        }
        .setNegativeButton(R.string.button_cancel, null)
        .create()
    dialog.show()
    dialog.window?.findViewById<TextInputEditText>(android.R.id.text1)
        ?.let {
            it.setText(node.name.toCharArray(), 0, node.name.length)
            if (node.name.isNotEmpty() && node.name.lastIndexOf(".") > 1) {
                it.setSelection(0, node.name.lastIndexOf("."))
            }
            it.requestFocus()
        }
    return true
}

private fun doRename(
    context: Context,
    node: RTreeNode,
    name: CharSequence,
    nodeService: NodeService,
) {
    CellsApp.instance.appScope.launch {
        nodeService.rename(StateID.fromId(node.encodedState), name.toString())
            ?.let {
                withContext(Dispatchers.Main) {
                    showLongMessage(context, it)
                }
            }
    }
}
