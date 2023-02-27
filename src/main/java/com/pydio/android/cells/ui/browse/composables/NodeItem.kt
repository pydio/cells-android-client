package com.pydio.android.cells.ui.browse.composables

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.aaLegacy.bindings.getMessageFromLocalModifStatus
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.api.SdkNames
import com.pydio.cells.utils.Str

@Composable
fun NodeItem(
    item: RTreeNode,
    mime: String,
    sortName: String?,
    title: String,
    desc: String,
    isBookmarked: Boolean,
    isOfflineRoot: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_thumb_margin)),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Thumbnail(item)

            Column(
                modifier = modifier
                    .weight(1f)
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isBookmarked) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_star_border_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator)),
                    contentDescription = ""
                    // contentScale = ContentScale.Crop,
                )
            }
            if (isShared) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_link_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagShare),
                    contentDescription = "",
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
                )
            }
            if (isOfflineRoot) {
                Image(
                    painter = painterResource(R.drawable.ic_outline_download_done_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagOffline),
                    contentDescription = "",
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
                )
            }

            Surface(Modifier.clickable { more() }) {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_button_size))
                )
            }
        }
    }
}

@Composable
fun OfflineRootItem(
    item: RLiveOfflineRoot,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {

    OfflineRootItem(
        item.encodedState,
        item.sortName,
        item.name,
        title,
        desc,
        item.mime,
        item.etag,
        item.hasThumb(),
        item.isBookmarked(),
        item.isShared(),
        more = more,
    )
}

@Composable
fun OfflineRootItem(
    encodedState: String,
    sortName: String?,
    name: String,
    title: String,
    desc: String,
    mime: String,
    etag: String?,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_thumb_margin)),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Thumbnail(encodedState, sortName, name, mime, etag, hasThumb)

            Column(
                modifier = modifier
                    .weight(1f)
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (isBookmarked) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_star_border_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator)),
                    contentDescription = ""
                    // contentScale = ContentScale.Crop,
                )
            }
            if (isShared) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_link_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagShare),
                    contentDescription = "",
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
                )
            }

            Surface(Modifier.clickable { more() }) {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_button_size))
                )
            }
        }
    }
}


@Composable
fun getNodeTitle(name: String, mime: String): String {
    return if (SdkNames.NODE_MIME_RECYCLE == mime) {
        stringResource(R.string.recycle_bin_label)
    } else {
        name
    }
}

@Composable
fun getNodeDesc(
    context: Context,
    remoteModificationTS: Long,
    size: Long,
    localModificationStatus: String?,
): String {

    if (Str.notEmpty(localModificationStatus)) {
        getMessageFromLocalModifStatus(context, localModificationStatus!!)?.let {
            return it
        }
    }
    val timestamp = DateUtils.formatDateTime(
        context,
        remoteModificationTS * 1000L,
        DateUtils.FORMAT_ABBREV_RELATIVE
    )
    val sizeStr = Formatter.formatShortFileSize(context, size)
    return "$timestamp • $sizeStr"
}
