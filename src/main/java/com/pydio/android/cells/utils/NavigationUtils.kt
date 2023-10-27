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

private const val LOG_TAG = "NavigationUtils.kt"

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
    Log.i(LOG_TAG, "... Launch external view for ${node.getStateID()}")

    val uri = FileProvider.getUriForFile(context, DEFAULT_FILE_PROVIDER_ID, file)

    // Managing the mime
    var mime = node.mime

    // This workaround addresses a legacy corner case:
    //  TODO Insure the bug is really fixed and get rid of this.
    if (Str.notEmpty(mime)) {
        //   Mime was not correctly handled in old JavaSDK and we sometimes retrieved a mime type
        //   surrounded by unnecessary double quotes that break the view intent. Should fixed by now.
        if (mime.startsWith("\"") && mime.endsWith("\"")) {
            Log.e(LOG_TAG, "About to manually remove superfluous double quote from [$mime]")

            mime = mime.substring(1)
            mime = mime.substring(0, mime.length - 1)
            Log.e(LOG_TAG, "Manually removed superfluous double quote")
            Thread.dumpStack()
        }
    }

    // Mime is still not set, try with the contentResolver.
    if (SdkNames.NODE_MIME_DEFAULT == mime) {
        val cr = CellsApp.instance.applicationContext.contentResolver
        mime = cr.getType(uri) ?: SdkNames.NODE_MIME_DEFAULT
        Log.d(LOG_TAG, "... Retrieved mime: $mime with contentResolver")
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
        Log.e(LOG_TAG, "No app found to open ${file.name}")
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
