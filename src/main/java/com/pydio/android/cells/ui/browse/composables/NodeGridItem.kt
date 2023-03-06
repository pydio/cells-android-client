package com.pydio.android.cells.ui.browse.composables

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.GridThumb
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme

private const val logTag = "NodeGridItem"

@Composable
fun NodeGridItem(
    item: RTreeNode,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    NodeGridItem(
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
fun NodeGridItem(
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

    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 0.dp,
    )
    val descPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 0.dp,
        bottom = 8.dp,
    )
    Card(
        //shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        modifier = modifier
    ) {

        GridThumb(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            outerSize = dimensionResource(R.dimen.grid_layout_card_icon_size),
            iconSize = dimensionResource(R.dimen.grid_icon_size),
            clipShape = RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(titlePadding)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(descPadding)
        )
    }
}


@Composable
fun NodeGridItemBox(
    item: RTreeNode,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
//    Log.e(logTag, item.encodedState)
//    Log.e(logTag, "${item.sortName}")
//    Log.e(logTag, item.name)
//    Log.e(logTag, title)
//    Log.e(logTag, desc)
//    Log.e(logTag, item.mime)
//    Log.e(logTag, "${item.etag}")

    NodeGridItemBox(
        title = title,
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
fun NodeGridItemBox(
    title: String,
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
    modifier: Modifier,
) {

    Box(modifier = modifier.size(dimensionResource(R.dimen.grid_col_min_width))) {
        GridThumb(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            outerSize = dimensionResource(R.dimen.grid_col_min_width),
            iconSize = dimensionResource(R.dimen.grid_icon_size),
            clipShape = RoundedCornerShape(0.dp),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f))
                .padding(all = dimensionResource(R.dimen.grid_col_text_padding))
        )

        Column(Modifier.padding(all = dimensionResource(R.dimen.grid_col_text_padding))) {
            if (isBookmarked) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_star_border_24),
                    colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
                    modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator)),
                    contentDescription = ""
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
        }

        IconButton(
            onClick = { more() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .2f))
        ) {
            Icon(
                imageVector = CellsIcons.MoreVert,
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.list_button_size)),
            )
        }
    }
}

@Composable
fun OfflineRootGridItem(
    item: RLiveOfflineRoot,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {

    OfflineRootGridItem(
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
fun OfflineRootGridItem(
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

    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 0.dp,
    )
    val descPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 0.dp,
        bottom = 8.dp,
    )

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        modifier = modifier
// This is the outer padding of the card
//            .padding(
//            start = 8.dp,
//            end = 8.dp,
//            top = 0.dp,
//            bottom = 12.dp,
//        )
    ) {
        GridThumb(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            outerSize = dimensionResource(R.dimen.grid_ws_image_size),
            iconSize = dimensionResource(R.dimen.grid_icon_size),
            clipShape = RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(titlePadding)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(descPadding)
        )
    }
}

@Composable
fun OfflineRootLargeGridItem(
    item: RLiveOfflineRoot,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {

    OfflineRootLargeGridItem(
        encodedState = item.encodedState,
        sortName = item.sortName,
        name = item.name,
        title = title,
        desc = desc,
        mime = item.mime,
        eTag = item.etag,
        hasThumb = item.hasThumb(),
        modifier = modifier,
    )
}

@Composable
fun OfflineRootLargeGridItem(
    encodedState: String,
    sortName: String?,
    name: String,
    title: String,
    desc: String,
    mime: String,
    eTag: String?,
    hasThumb: Boolean,
    modifier: Modifier
) {

    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 0.dp,
    )
    val descPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 0.dp,
        bottom = 8.dp,
    )

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        modifier = modifier
    ) {
        GridThumb(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            outerSize = dimensionResource(R.dimen.grid_ws_image_size),
            iconSize = dimensionResource(R.dimen.grid_large_icon_size),
            clipShape = RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)),
        )
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
            thickness = 0.3.dp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(titlePadding)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(descPadding)
        )
    }
}

@Preview("NodeGridItemPreview Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "NodeGridItemPreview Dark Mode"
)
@Composable
private fun NodeGridItemPreview() {
    val encodedState =
        "bruno@https%3A%2F%2Fandroid.ci.pyd.io@%2Fcommon-files%2FFlowers%2FIMG_20220508_172716.jpg"
    val sortName = "5_IMG_20220508_172716.jpg"
    val name = "IMG_20220508_172716.jpg"
    val title = "IMG_20220508_172716.jpg"
    val desc = "November 14, 2022 • 2.0 MB"
    val mime = "image/jpeg"
    val eTag = "3d280d2e133f075521d1f2697e55c49e"
    val hasThumb = false
    val isBookmarked = false
    val isShared = false
    val isOfflineRoot = false

    CellsTheme {
        NodeGridItem(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            title = title,
            desc = desc,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            isBookmarked = isBookmarked,
            isShared = isShared,
            isOfflineRoot = isOfflineRoot,
            more = {},
            modifier = Modifier,
        )
    }
}

@Preview("OfflineRootGridItemPreview Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "FolderTopBar Dark Mode"
)
@Composable
private fun OfflineRootGridItemPreview() {
    val encodedState =
        "bruno@https%3A%2F%2Fandroid.ci.pyd.io@%2Fcommon-files%2FImages+with+a+very+long+name"
    val sortName = "3_Images with a very long name"
    val name = "Images with a very long name"
    val title = "Images with a very long name"
    val desc = "Last check: 20 hours ago • 16 MB"
    val mime = "pydio/nodes-list"
    val eTag = "92a05680b0066fbf6d418b882225e0dc"
    val hasThumb = false
    val isBookmarked = false
    val isShared = false

    CellsTheme {
        OfflineRootGridItem(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            title = title,
            desc = desc,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            isBookmarked = isBookmarked,
            isShared = isShared,
            more = {},
            Modifier
        )
    }
}



