package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

private const val logTag = "RecycleMoreMenuView"

@Composable
fun OfflineMoreMenuView(
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (NodeAction) -> Unit,
    tint: Color,
    bgColor: Color,
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
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.OpenLocation,
                title = stringResource(R.string.open_in_workspaces),
                onItemClick = { launch(NodeAction.OpenParentLocation) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.DownloadToDevice,
                title = stringResource(R.string.download_to_device),
                onItemClick = { launch(NodeAction.DownloadToDevice) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.KeepOffline,
                title = "Forget", // TODO i18n
                onItemClick = { launch(NodeAction.ToggleOffline(false)) },
                tint = tint,
                bgColor = bgColor,
            )
        }
    }
}

