package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

// private const val logTag = "OfflineMoreMenuView"

@Composable
fun OfflineMenu(
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (NodeAction) -> Unit,
) {
//    Log.e(logTag, "### encoded id for $stateID: ${stateID.id}")
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            BottomSheetHeader(
                thumb = { Thumbnail(rTreeNode) },
                title = stateID.fileName ?: "",
                desc = stateID.parentPath,
            )
        }
        item { BottomSheetDivider() }

        item {
            BottomSheetListItem(
                icon = CellsIcons.Refresh,
                title = stringResource(R.string.force_resync),
                onItemClick = { launch(NodeAction.ForceResync) },
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.OpenLocation,
                title = if (rTreeNode.isFile()) {
                    stringResource(R.string.open_parent_in_workspaces)
                } else {
                    stringResource(R.string.open_in_workspaces)
                },
                onItemClick = { launch(NodeAction.OpenInApp) },
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
                icon = CellsIcons.KeepOffline,
                title = stringResource(R.string.remove_from_offline),
                onItemClick = { launch(NodeAction.ToggleOffline(false)) },
            )
        }
    }
}
