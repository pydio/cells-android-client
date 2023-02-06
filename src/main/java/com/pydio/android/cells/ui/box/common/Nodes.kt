package com.pydio.android.cells.ui.box.common

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.bindings.getMessageFromLocalModifStatus
import com.pydio.cells.api.SdkNames
import com.pydio.cells.utils.Str

fun getNodeTitle(name: String, mime: String): String {
    return if (SdkNames.NODE_MIME_RECYCLE == mime) {
        // Todo rather use a composable here to have resources
        "Recycle Bin"
    } else {
        name
    }
}

fun getNodeDesc(
    ctx: Context,
    item: RTreeNode,
): String {

    if (Str.notEmpty(item.localModificationStatus)) {
        getMessageFromLocalModifStatus(ctx, item.localModificationStatus!!)?.let {
            return it
        }
    }
    val mTimeValue = DateUtils.formatDateTime(
        ctx,
        item.remoteModificationTS * 1000L,
        DateUtils.FORMAT_ABBREV_RELATIVE
    )
    val sizeValue = Formatter.formatShortFileSize(ctx, item.size)
    return "$mTimeValue • $sizeValue"
}
