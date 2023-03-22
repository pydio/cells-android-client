package com.pydio.android.cells.ui.browse.menus

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.core.composables.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.BottomSheetListItemWithToggle
import com.pydio.android.cells.ui.core.composables.DefaultTitleText
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.menus.SimpleMenuItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

//private const val logTag = "SingleNodeMenu"

@Composable
fun SingleNodeMenu(
    stateID: StateID,
    rTreeNode: RTreeNode,
    rWorkspace: RWorkspace?,
    launch: (NodeAction) -> Unit,
) {
    // TODO handle case when offline

    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            val title = stateID.fileName ?: run {
                rWorkspace?.label
            }
            val desc = stateID.parentPath ?: run {
                "${stateID.username}@${stateID.serverUrl}"
            }
            BottomSheetHeader(
                thumb = { Thumbnail(rTreeNode) },
                title = title ?: "",
                desc = desc,
            )
        }
        item {
            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.bottom_sheet_item_h_padding))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
        }
        if (rTreeNode.isFile()) {
            item {
                BottomSheetListItem(
                    icon = CellsIcons.DownloadToDevice,
                    title = stringResource(R.string.download_to_device),
                    onItemClick = { launch(NodeAction.DownloadToDevice) },
                )
            }
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Rename,
                title = stringResource(R.string.rename),
                onItemClick = { launch(NodeAction.Rename) },
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.CopyTo,
                title = stringResource(R.string.copy_to),
                onItemClick = { launch(NodeAction.CopyTo) },
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.MoveTo,
                title = stringResource(R.string.move_to),
                onItemClick = { launch(NodeAction.MoveTo) },
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Delete,
                title = stringResource(R.string.delete),
                onItemClick = { launch(NodeAction.Delete) },
            )
            BottomSheetDivider()
        }


        item {
            BottomSheetListItemWithToggle(
                icon = CellsIcons.Bookmark,
                title = stringResource(R.string.bookmark),
                isSelected = rTreeNode.isBookmarked(),
                onItemClick = { launch(NodeAction.ToggleBookmark(it)) },
            )
        }
        item {
            BottomSheetListItemWithToggle(
                icon = CellsIcons.KeepOffline,
                title = stringResource(R.string.keep_offline),
                isSelected = rTreeNode.isOfflineRoot(),
                onItemClick = { launch(NodeAction.ToggleOffline(it)) },
            )
        }
        if (rTreeNode.isShared()) {
            item {
                BottomSheetDivider()
                DefaultTitleText(
                    text = stringResource(R.string.public_link),
                )
            }

            item {
                // TODO provide a better interface for this
                Column(Modifier.padding(start = dimensionResource(id = R.dimen.margin))) {
                    BottomSheetListItem(
                        icon = CellsIcons.Share,
                        title = stringResource(R.string.share_with),
                        onItemClick = { launch(NodeAction.ShareWith) },
                    )
                    BottomSheetListItem(
                        icon = CellsIcons.CopyTo,
                        title = stringResource(R.string.copy_to_clipboard),
                        onItemClick = { launch(NodeAction.CopyToClipboard) },
                    )
                    BottomSheetListItem(
                        icon = CellsIcons.QRCode,
                        title = stringResource(R.string.display_as_qrcode),
                        onItemClick = { launch(NodeAction.ShowQRCode) },
                    )
                    BottomSheetListItem(
                        icon = CellsIcons.Delete,
                        title = stringResource(R.string.remove_link),
                        onItemClick = { launch(NodeAction.RemoveLink) },
                    )
                }
            }
        } else {
            item {
                BottomSheetListItemWithToggle(
                    icon = CellsIcons.Link,
                    title = stringResource(R.string.public_link),
                    isSelected = rTreeNode.isShared(),
                    onItemClick = { if (it) launch(NodeAction.CreateShare) },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TreeNodeBottomSheetPreview() {
    val context = LocalContext.current
    val onClick: (String) -> Unit = { title ->
        Toast.makeText(
            context, title, Toast.LENGTH_SHORT
        ).show()
    }
    val simpleMenuItems: List<SimpleMenuItem> = listOf(
        SimpleMenuItem(CellsIcons.Share, "Share", { onClick("Share") }),
        SimpleMenuItem(CellsIcons.Link, "Get Link", { onClick("Get Link") }),
        SimpleMenuItem(CellsIcons.Edit, "Edit", { onClick("Edit") }, true),
        SimpleMenuItem(CellsIcons.Delete, "Delete", { onClick("Delete") }),
    )

    BottomSheetContent(
        {
            BottomSheetHeader(
                icon = CellsIcons.Processing,
                title = "My Transfer of jpg.pdf",
                desc = "45MB, started at 5.54 AM, 46% done"
            )
        },
        simpleMenuItems
    )
}
