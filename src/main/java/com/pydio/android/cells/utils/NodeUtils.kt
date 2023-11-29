package com.pydio.android.cells.utils

import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.runtime.saveable.Saver
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.WorkspaceNode
import com.pydio.cells.transport.StateID

private const val LOG_TAG = "NodeUtils.kt"

// Utility to make the stateID savable
val stateIDSaver = Saver<StateID, String>(
    save = { stateID -> stateID.id },
    restore = { id -> StateID.fromId(id) },
)

fun areNodeContentEquals(remote: FileNode, local: RTreeNode, legacy: Boolean): Boolean {
    // TODO rather use this when debugging is over. Also adapt areWsNodeContentEquals(), see below
//        return remote.eTag != null
//                && remote.eTag == local.etag
//                && local.remoteModificationTS == remote.lastModified()
//                // Also compare meta hash: timestamp is not updated when a meta changes
//                && remote.metaHashCode == local.metaHash
// // Does not work
// // && remote.flags == local.flags

    var isEqual: Boolean

    isEqual = local.remoteModificationTS == remote.lastModified
    if (!isEqual) {
        //Log.d(NODE_UTILS, "Differ: modification times are not equals")
        return false
    }

    if (!legacy) { // We can rely on the ETag if remote is a Cells leaf node.
        isEqual = remote.eTag != null
        if (!isEqual) {
            Log.d(LOG_TAG, "Differ: no remote eTag")
            return false
        }
        isEqual = remote.eTag == local.etag
        if (!isEqual) {
            Log.d(LOG_TAG, "Differ: eTag are different")
            return false
        }
    }

    // Also compare meta hash: timestamp is not updated when a meta changes
    isEqual = remote.metaHashCode == local.metaHash
    if (!isEqual) {
        Log.d(LOG_TAG, "Differ: meta hash are not equals")
        Log.d(LOG_TAG, "local meta: ${local.properties}")
        Log.d(LOG_TAG, "remote meta: ${remote.properties}")
        return false
    }

    // TODO when miss modifications when the offline flag is set.
    // isEqual = remote.flags == local.flags

    return true
}

fun areWsNodeContentEquals(remote: WorkspaceNode, local: RWorkspace): Boolean {

    var isEqual = remote.slug == local.slug
    if (!isEqual) {
        Log.d(LOG_TAG, "Differ: slug have changed")
        return false
    }
    isEqual = remote.label == local.label
    if (!isEqual) {
        Log.d(LOG_TAG, "Differ: labels are different")
        return false
    }
    isEqual = remote.description == local.description
    if (!isEqual) {
        Log.d(LOG_TAG, "Differ: descriptions are different")
        return false
    }

    isEqual = local.remoteModificationTS == remote.lastModified
    if (!isEqual) {
        Log.d(LOG_TAG, "Differ: Modification time are not equals")
        return false
    }
    return true
}

fun isPreViewable(element: RTreeNode): Boolean {
    return if (element.mime.startsWith("image/") ||
        // TODO remove this once the mime has been cleaned
        element.mime.startsWith("\"image/")
    ) {
        true
    } else if (element.mime == SdkNames.NODE_MIME_DEFAULT || element.mime == "\"${SdkNames.NODE_MIME_DEFAULT}\"") {
        val name = element.name.lowercase()
        name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".gif")
    } else {
        false
    }
}

fun getMimeType(url: String, fallback: String = SdkNames.NODE_MIME_DEFAULT): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(url)
    if (ext != null) {
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (mime != null) return mime
    }
    return fallback
}
