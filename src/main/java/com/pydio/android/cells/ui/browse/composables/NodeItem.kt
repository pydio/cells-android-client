package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.UseCellsTheme

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

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                top = 8.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 8.dp,
            )
    ) {

        Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (isBookmarked) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_star_border_24),
                colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator)),
                contentDescription = ""
            )
        }
        if (isShared) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_link_24),
                colorFilter = ColorFilter.tint(CellsColor.flagShare),
                contentDescription = "",
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator))
            )
        }
        if (isOfflineRoot) {
            Image(
                painter = painterResource(R.drawable.ic_outline_download_done_24),
                colorFilter = ColorFilter.tint(CellsColor.flagOffline),
                contentDescription = "",
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator))
            )
        }

        Box(
            Modifier
                .clickable { more() }
        ) {
            Icon(
                //imageVector = CellsIcons.MoreVert,
                painter = painterResource(id = R.drawable.aa_300_more_vert_40px),
                contentDescription = stringResource(id = R.string.open_more_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
            )
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

    M3NodeItem(
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
        isOfflineRoot = true,
        more = more,
        modifier = modifier,
    )
}

//@Composable
//fun OfflineRootItem(
//    encodedState: String,
//    sortName: String?,
//    name: String,
//    title: String,
//    desc: String,
//    mime: String,
//    eTag: String?,
//    hasThumb: Boolean,
//    isBookmarked: Boolean,
//    isShared: Boolean,
//    more: () -> Unit,
//    modifier: Modifier
//) {
//    Surface(
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(all = dimensionResource(R.dimen.card_padding))
//    ) {
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_thumb_margin)),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//
//            Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb)
//
//            Column(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(
//                        horizontal = dimensionResource(R.dimen.card_padding),
//                        vertical = dimensionResource(R.dimen.margin_xsmall)
//                    )
//                    .wrapContentWidth(Alignment.Start)
//            ) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.bodyMedium,
//                )
//                Text(
//                    text = desc,
//                    style = MaterialTheme.typography.bodySmall,
//                )
//            }
//
//            if (isBookmarked) {
//                Image(
//                    painter = painterResource(R.drawable.ic_baseline_star_border_24),
//                    colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
//                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator)),
//                    contentDescription = ""
//                )
//            }
//            if (isShared) {
//                Image(
//                    painter = painterResource(R.drawable.ic_baseline_link_24),
//                    colorFilter = ColorFilter.tint(CellsColor.flagShare),
//                    contentDescription = "",
//                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
//                )
//            }
//
//            Surface(Modifier.clickable { more() }) {
//                Icon(
//                    imageVector = CellsIcons.MoreVert,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
//                )
//            }
//        }
//    }
//}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun M3ListItem(
//    title: String,
//    desc: String,
//    encodedState: String,
//    name: String,
//    sortName: String?,
//    mime: String,
//    eTag: String?,
//    hasThumb: Boolean,
//    isBookmarked: Boolean,
//    isOfflineRoot: Boolean,
//    isShared: Boolean,
//    more: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    ListItem(
//        modifier = modifier,
//        headlineText = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
//        supportingText = { Text(desc, maxLines = 1, overflow = TextOverflow.Ellipsis) },
//        leadingContent = { Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb) },
//        trailingContent = {
//            Surface(Modifier.clickable { more() }) {
//                Icon(
//                    // imageVector = CellsIcons.MoreVert,
//                    painter = painterResource(id = R.drawable.aa_300_more_vert_40px),
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
//                )
//            }
//        },
////        colors: ListItemColors = ListItemDefaults.colors(),
////        tonalElevation: Dp = ListItemDefaults.Elevation,
////        shadowElevation: Dp = ListItemDefaults.Elevation
//    )
//}

@Preview
@Composable
fun NodeItemPreview() {
    val title = "Images with a very long name but very very very long. And this is the END!"
    val desc = "January 20 • 70 MB"
    val encodedState =
        "bruno@https%3A%2F%2Fandroid.ci.pyd.io@%2Fcommon-files%2FImages+with+a+very+long+name+but+very+very+very+long.+And+this+is+the+END%21"
    val name = "Images with a very long name but very very very long. And this is the END!"
    val sortName = "3_Images with a very long name but very very very long. And this is the END!"
    val mime = "pydio/nodes-list"
    val eTag = "92a05680b0066fbf6d418b882225e0dc"
    val hasThumb = false
    val isBookmarked = true
    val isOfflineRoot = false
    val isShared = true
    UseCellsTheme {
        M3NodeItem(
            title = title,
            desc = desc,
            encodedState = encodedState,
            name = name,
            sortName = sortName,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            isBookmarked = isBookmarked,
            isOfflineRoot = isOfflineRoot,
            isShared = isShared,
            more = { },
            modifier = Modifier,
        )
    }
}

