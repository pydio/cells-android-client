package com.pydio.android.cells.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.pydio.android.cells.BuildConfig
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.io.File

const val DEFAULT_FILE_PROVIDER_SUFFIX = ".fileprovider"
const val DEFAULT_FILE_PROVIDER_ID =
    BuildConfig.APPLICATION_ID + DEFAULT_FILE_PROVIDER_SUFFIX

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun showLongMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

/**
 * Open current file with the viewer provided by Android OS.
 *  TODO insure the user can choose the opening app,
 *    see: https://developer.android.com/guide/components/intents-filters#ForceChooser
 *    and https://developer.android.com/training/basics/intents/sending
 */
fun externallyView(context: Context, file: File, node: RTreeNode): Boolean {
    val logTag = "externallyView"
    var mime = node.mime
    if (Str.notEmpty(mime)) {
        // This workarounds address a corner case when the mime was not correctly
        //   handled in ancestors layers SDK Java and that should fixed by now:
        //   we sometimes retrieved a mime type surrounded by unnecessary double quotes that break the view intent
        //  TODO Insure the bug is really fixed and get rid of this.
        if (mime.startsWith("\"") && mime.endsWith("\"")) {
            Log.e(logTag, "About to manually remove superfluous double quote from [$mime]")

            mime = mime.substring(1)
            mime = mime.substring(0, mime.length - 1)
            Log.e(logTag, "Manually removed superfluous double quote")
            Thread.dumpStack()
        }
    }

    // First approach fails when name has spaces or quotes...
    val uri = FileProvider.getUriForFile(context, DEFAULT_FILE_PROVIDER_ID, file)
    if (SdkNames.NODE_MIME_DEFAULT == mime) {
        Log.d(logTag, "... Last chance with the content resolver")
        val cr = CellsApp.instance.applicationContext.contentResolver
        mime = cr.getType(uri) ?: SdkNames.NODE_MIME_DEFAULT
    }

    val viewIntent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val title: String = context.resources.getString(R.string.choose_external_app_title)
    // Force to show the chooser dialog
    val chooser: Intent = Intent.createChooser(viewIntent, title)
    try {
        startActivity(context, chooser, null)
//        if (BuildConfig.DEBUG) { // Debug only
//            val msg = "Opened ${file.name} with external viewer"
//            Log.e(logTag, "Intent success: $msg")
//        }
        return true
    } catch (e: ActivityNotFoundException) {
        showMessage(context, "No app found to open ${file.name}")
    }

    return false
}

@Deprecated("Does not do anything anymore")
fun resetToHomeStateIfNecessary(manager: FragmentManager, currentState: StateID) {
    // We manually set the current to be at root of the workspace to handle certain corner cases,
    // typically when app has been restored with an empty back stack deep in a workspace or
    // when we are in a special page
    val count = manager.backStackEntryCount
    if (count == 0 && currentState.path?.length ?: 0 > 0
        || currentState.path == "/${currentState.slug}"
    ) {
//        CellsApp.instance.setCurrentState(StateID.fromId(currentState.accountId))
    }
}

@Deprecated("Should probably be useless alo now.")
fun dumpBackStack(caller: String?, manager: FragmentManager) {
    val count = manager.backStackEntryCount
    val entry = if (count > 0) manager.getBackStackEntryAt(count - 1) else null
    Log.i(caller, "Back stack entry count: $count")
    Log.i(caller, "Previous entry: $entry")
}
