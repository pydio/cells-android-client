package com.pydio.android.cells.utils

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.WorkspaceNode
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode

private const val NODE_UTILS = "NodeUtils"

fun areNodeContentEquals(remote: FileNode, local: RTreeNode, legacy: Boolean): Boolean {
    // TODO rather use this when debugging is over.
//        return remote.eTag != null
//                && remote.eTag == local.etag
//                && local.remoteModificationTS == remote.lastModified()
//                // Also compare meta hash: timestamp is not updated when a meta changes
//                && remote.metaHashCode == local.metaHash

    var isEqual: Boolean

    isEqual = local.remoteModificationTS == remote.getLastModified()
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: Modif time are not equals")
        return false
    }

    if (!legacy)  { // We can rely on the ETag if remote is a Cells leaf node.
        isEqual = remote.eTag != null
        if (!isEqual) {
            Log.d(NODE_UTILS, "Differ: no remote eTag")
            return false
        }
        isEqual = remote.eTag == local.etag
        if (!isEqual) {
            Log.d(NODE_UTILS, "Differ: eTag are different")
            return false
        }
    }

    // Also compare meta hash: timestamp is not updated when a meta changes
    isEqual = remote.metaHashCode == local.metaHash
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: meta hash are not equals")
        Log.d(NODE_UTILS, "local meta: ${local.properties}")
        Log.d(NODE_UTILS, "remote meta: ${remote.properties}")
        return false
    }
    return true
}

fun areWsNodeContentEquals(remote: WorkspaceNode, local: RWorkspace): Boolean {
    // TODO rather use this when debugging is over.
//        return remote.eTag != null
//                && remote.eTag == local.etag
//                && local.remoteModificationTS == remote.lastModified()
//                // Also compare meta hash: timestamp is not updated when a meta changes
//                && remote.metaHashCode == local.metaHash

    var isEqual = remote.slug == local.slug
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: slug have changed")
        return false
    }
    isEqual = remote.label == local.label
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: labels are different")
        return false
    }
    isEqual = remote.description == local.description
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: descriptions are different")
        return false
    }

    isEqual = local.remoteModificationTS == remote.getLastModified()
    if (!isEqual) {
        Log.d(NODE_UTILS, "Differ: Modif time are not equals")
        return false
    }
//    // Also compare meta hash: timestamp is not updated when a meta changes
//    isEqual = remote.metaHashCode == local.metaHash
//    if (!isEqual) {
//        Log.d(NODE_UTILS, "Differ: meta hash are not equals")
//        Log.d(NODE_UTILS, "local meta: ${local.meta}")
//        Log.d(NODE_UTILS, "remote meta: ${remote.properties}")
//        return false
//    }

    return true
}


fun getAppMime(context: Context, name: String): String {
    val filename = name.lowercase()

    return if (filename.contains(".doc") || filename.contains(".docx")) {
        "application/msword"
    } else if (filename.contains(".pdf")) {
        "application/pdf"
    } else if (filename.contains(".ppt") || filename.contains(".pptx")) {
        "application/vnd.ms-powerpoint"
    } else if (filename.contains(".xls") || filename.contains(".xlsx")) {
        "application/vnd.ms-excel"
    } else if (filename.contains(".rtf")) {
        "application/rtf"
    } else if (filename.contains(".wav") || filename.contains(".mp3")) {
        "audio/x-wav"
    } else if (filename.contains(".ogg") || filename.contains(".flac")) {
        "audio/*"
    } else if (filename.contains(".gif")) {
        "image/gif"
    } else if (filename.contains(".jpg") || filename.contains(".jpeg") || filename.contains(".png")) {
        "image/jpeg"
    } else if (filename.contains(".txt")) {
        "text/plain"
    } else if (filename.contains(".3gp") || filename.contains(".mpg") || filename
            .contains(".mpeg") || filename.contains(".mpe") || filename
            .contains(".mp4") || filename.contains(".avi")
    ) {
        "video/*"
    } else {
        "*/*"
    }
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

