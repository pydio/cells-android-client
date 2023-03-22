package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.style.TextOverflow
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.api.SdkNames

@Composable
fun NodeItem(
    item: RTreeNode,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    M3NodeItem(
        title = title,
        desc = desc,
        encodedState = item.encodedState,
        name = item.name,
        sortName = item.sortName,
        mime = item.mime,
        eTag = item.etag,
        hasThumb = item.hasThumb(),
        isBookmarked = item.isBookmarked(),
        isOfflineRoot = item.isOfflineRoot(),
        isShared = item.isShared(),
        more = more,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3NodeItem(
    title: String,
    desc: String,
    encodedState: String,
    name: String,
    sortName: String?,
    mime: String,
    eTag: String?,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isOfflineRoot: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        headlineText = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingText = { Text(desc, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb) },
        trailingContent = {
            Surface(Modifier.clickable { more() }) {
                Icon(
                    // imageVector = CellsIcons.MoreVert,
                    painter = painterResource(id = R.drawable.aa_300_more_vert_40px),
                    contentDescription = null, // TODO
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }
        },
//        colors: ListItemColors = ListItemDefaults.colors(),
//        tonalElevation: Dp = ListItemDefaults.Elevation,
//        shadowElevation: Dp = ListItemDefaults.Elevation
    )
}

@Composable
fun NodeItem(
    title: String,
    desc: String,
    encodedState: String,
    name: String,
    sortName: String?,
    mime: String,
    eTag: String?,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isOfflineRoot: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_thumb_margin)),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb)

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
                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
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
        encodedState = item.encodedState,
        sortName = item.sortName,
        name = item.name,
        title = title,
        desc = desc,
        mime = item.mime,
        eTag = item.etag,
        hasThumb = item.hasThumb(),
        isBookmarked = item.isBookmarked(),
        isShared = item.isShared(),
        more = more,
        modifier = modifier,
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
    eTag: String?,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier
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

            Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb)

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
                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
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

