package com.pydio.android.cells.tasks

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.R
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showMessage

fun createFolder(
    context: Context,
    parentId: StateID,
    nodeService: NodeService,
): Boolean {

    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.dialog_create_folder_title)
        .setView(R.layout.dialog_edit_text)
        .setPositiveButton(R.string.dialog_create_folder_positive_btn) { dialog, _ ->
            val input = (dialog as AlertDialog).findViewById<TextView>(android.R.id.text1)
            doCreateFolder(context, parentId, input!!.text, nodeService)
        }
        .setNegativeButton(R.string.button_cancel, null)
        .show()
    return true
}

private fun doCreateFolder(
    context: Context,
    parentId: StateID,
    name: CharSequence,
    nodeService: NodeService,
) {
    CellsApp.instance.appScope.launch {
        nodeService.createFolder(
            parentId,
            name.toString()
        )
            ?.let {
                withContext(Dispatchers.Main) {
                    showMessage(context, it)
                }
            }
            ?: run {
                withContext(Dispatchers.Main) {
                    showMessage(
                        context,
                        "Folder created at ${parentId.file}."
                    )
                }
            }
    }
}
