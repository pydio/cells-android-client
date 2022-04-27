package com.pydio.android.cells.tasks

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.pydio.android.cells.db.nodes.RTreeNode

fun getOpenFilePickerIntent(node: RTreeNode, pickerInitialUri: Uri?): Intent {

    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = node.mime
        putExtra(Intent.EXTRA_TITLE, node.name)

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker before your app creates the document.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pickerInitialUri?.let {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
    }
}
