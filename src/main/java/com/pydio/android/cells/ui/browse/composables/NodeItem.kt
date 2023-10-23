package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.pydio.android.cells.ui.core.composables.M3IconThumb
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun NodeItem(
    item: TreeNodeItem,
    title: String,
    desc: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    M3NodeItem(
        stateID = item.stateID,
        title = title,
        desc = desc,
        name = item.name,
        sortName = item.sortName,
        mime = item.mime,
        eTag = item.eTag,
        metaHash = item.metaHash,
        hasThumb = item.hasThumb,
        isBookmarked = item.isBookmarked,
        isOfflineRoot = item.isOfflineRoot,
        isShared = item.isShared,
        more = more,
        modifier = modifier,
        isSelectionMode = isSelectionMode,
        isSelected = isSelected,
    )
}

@Composable
fun M3NodeItem(
    stateID: StateID,
    title: String,
    desc: String,
    name: String,
    sortName: String?,
    mime: String,
    eTag: String?,
    metaHash: Int,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isOfflineRoot: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false
) {
    var lm = modifier.padding(0.dp, .2.dp)
    if (isSelected) {
        lm = lm.background(MaterialTheme.colorScheme.surfaceVariant)
    }
    lm = lm.padding(
        top = dimensionResource(R.dimen.list_item_inner_padding),
        bottom = dimensionResource(R.dimen.list_item_inner_padding),
        start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
        end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = lm
    ) {

        if (isSelected) {
            M3IconThumb(
                id = R.drawable.ic_baseline_check_24,
                color = MaterialTheme.colorScheme.surfaceTint
            )
        } else {

            Thumbnail(stateID, sortName, name, mime, eTag, metaHash, hasThumb)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            Text(
                text = title,
                color = textColor,
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

        if (!isSelectionMode) {

            if (isBookmarked) {
                Image(
                    imageVector = CellsIcons.ButtonFavorite,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator)),
                    contentDescription = ""
                )
            }
            if (isShared) {
                Image(
                    imageVector = CellsIcons.ButtonShare,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                    contentDescription = "",
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator))
                )
            }
            if (isOfflineRoot) {
                Image(
                    painter = painterResource(R.drawable.cloud_download_24px),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
                    contentDescription = "",
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_item_flag_decorator))
                )
            }

            IconButton(onClick = { more() }) {
                Icon(
                    painter = painterResource(id = R.drawable.aa_300_more_vert_40px),
                    contentDescription = stringResource(id = R.string.open_more_menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
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
    M3NodeItem(
        stateID = item.getStateID(),
        title = title,
        desc = desc,
        name = item.name,
        sortName = item.sortName,
        mime = item.mime,
        eTag = item.etag,
        metaHash = -1,
        hasThumb = item.hasThumb(),
        isBookmarked = item.isBookmarked(),
        isOfflineRoot = true,
        isShared = item.isShared(),
        more = more,
        modifier = modifier,
    )
}

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
    val metaHash = 667783986
    val hasThumb = false
    val isBookmarked = true
    val isOfflineRoot = false
    val isShared = true
    UseCellsTheme {
        M3NodeItem(
            stateID = StateID.fromId(encodedState),
            title = title,
            desc = desc,
            name = name,
            sortName = sortName,
            mime = mime,
            eTag = eTag,
            metaHash = metaHash,
            hasThumb = hasThumb,
            isBookmarked = isBookmarked,
            isOfflineRoot = isOfflineRoot,
            isShared = isShared,
            more = { },
            modifier = Modifier,
        )
    }
}
