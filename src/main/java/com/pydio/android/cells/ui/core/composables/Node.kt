package com.pydio.android.cells.ui.core.composables

import android.text.format.DateUtils
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.getMessageFromLocalModifStatus
import com.pydio.cells.api.SdkNames
import java.io.File

@Composable
fun getNodeTitle(name: String, mime: String): String {
    return if (SdkNames.NODE_MIME_RECYCLE == mime) {
        stringResource(R.string.recycle_bin_label)
    } else {
        name
    }
}

//@Composable
//fun getNodeTitle(name: String, mime: String): String {
//    return if (SdkNames.NODE_MIME_RECYCLE == mime) {
//        stringResource(R.string.recycle_bin_label)
//    } else {
//        name
//    }
//}

@Composable
fun getNodeDesc(
    item: RTreeNode,
): String {
    return getNodeDesc(
        remoteModificationTS = item.remoteModificationTS,
        size = item.size,
        localModificationStatus = item.localModificationStatus
    )
}

@Composable
fun getNodeDesc(
    remoteModificationTS: Long,
    size: Long,
    localModificationStatus: String?,
): String {
    return localModificationStatus?.let { getMessageFromLocalModifStatus(it) }
        ?: run {
            val timestamp = DateUtils.formatDateTime(
                LocalContext.current,
                remoteModificationTS * 1000L,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            val sizeStr = Formatter.formatShortFileSize(
                LocalContext.current,
                size
            )
            "$timestamp • $sizeStr"
        }
}


fun betterMime(passedMime: String, sortName: String?): String {
    return if (passedMime == SdkNames.NODE_MIME_DEFAULT) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(File("./$sortName").extension)
            ?: SdkNames.NODE_MIME_DEFAULT
    } else passedMime
}
