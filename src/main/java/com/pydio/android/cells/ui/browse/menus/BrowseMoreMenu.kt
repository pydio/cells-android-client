package com.pydio.android.cells.ui.browse.menus

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.android.cells.ui.core.composables.DefaultTitleText
import com.pydio.android.cells.ui.core.composables.M3IconThumb
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetFlagItem
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetNoAction
import com.pydio.android.cells.ui.core.composables.menus.SimpleMenuItem
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

//private const val logTag = "SingleNodeMenu"

@Composable
fun SingleNodeMenu(
    treeNodeVM: TreeNodeVM,
    connectionState: ConnectionState,
    stateID: StateID,
    nodeItem: TreeNodeItem,
    rWorkspace: RWorkspace?,
    launch: (NodeAction) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.bottom_sheet_v_spacing),
                bottom = dimensionResource(R.dimen.bottom_sheet_v_spacing).times(2)
            )
            .verticalScroll(scrollState)

    ) {
        // Header
        val title = stateID.fileName ?: run {
            rWorkspace?.label
        }
        val desc = stateID.parentPath ?: run {
            "${stateID.username}@${stateID.serverUrl}"
        }
        BottomSheetHeader(
            thumb = { Thumbnail(nodeItem) },
            title = title ?: "",
            desc = desc,
        )

        if (!nodeItem.isFolder && (connectionState.serverConnection.isConnected() || nodeItem.isCached)) {
            BottomSheetListItem(
                icon = CellsIcons.DownloadToDevice,
                title = stringResource(R.string.download_to_device),
                onItemClick = {
                    scope.launch {
                        if (nodeItem.isCached) {
                            launch(NodeAction.DownloadToDevice)
                        } else if (treeNodeVM.mustConfirmDL(
                                stateID,
                                connectionState.serverConnection
                            )
                        ) {
                            launch(NodeAction.ConfirmDownloadOnMetered)
                        } else {
                            launch(NodeAction.DownloadToDevice)
                        }
                    }
                },
            )
//            BottomSheetListItem(
//                icon = CellsIcons.DownloadToDevice,
//                title = stringResource(R.string.download_to_device),
//                onItemClick = { launch(NodeAction.DownloadToDevice) },
//            )
        }

        if (connectionState.serverConnection.isConnected()) {
            BottomSheetListItem(
                icon = CellsIcons.Rename,
                title = stringResource(R.string.rename),
                onItemClick = { launch(NodeAction.Rename) },
            )
            BottomSheetListItem(
                icon = CellsIcons.CopyTo,
                title = stringResource(R.string.copy_to),
                onItemClick = { launch(NodeAction.CopyTo) },
            )
            BottomSheetListItem(
                icon = CellsIcons.MoveTo,
                title = stringResource(R.string.move_to),
                onItemClick = { launch(NodeAction.MoveTo) },
            )
            BottomSheetListItem(
                icon = CellsIcons.Delete,
                title = stringResource(R.string.delete),
                onItemClick = { launch(NodeAction.Delete) },
            )
            BottomSheetDivider()
            BottomSheetFlagItem(
                nodeItem = nodeItem,
                icon = CellsIcons.Bookmark,
                title = stringResource(R.string.bookmark),
                flagType = AppNames.FLAG_BOOKMARK,
                onItemClick = { launch(NodeAction.ToggleBookmark(it)) },
            )
        }

        // This is local and does not require connection to the server
        BottomSheetFlagItem(
            nodeItem = nodeItem,
            iconId = R.drawable.cloud_download_24px,
            title = stringResource(R.string.keep_offline),
            flagType = AppNames.FLAG_OFFLINE,
            onItemClick = { launch(NodeAction.ToggleOffline(it)) },
        )

        if (nodeItem.isShared) {
            BottomSheetDivider()

            DefaultTitleText(
                text = stringResource(R.string.public_link),
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.bottom_sheet_start_padding),
                    end = dimensionResource(R.dimen.bottom_sheet_start_padding),
                    top = dimensionResource(R.dimen.bottom_sheet_v_spacing),
                    bottom = dimensionResource(R.dimen.bottom_sheet_v_padding),
                ),
            )

            // TODO provide a better user interface for this
            // Column(Modifier.padding(start = dimensionResource(id = R.dimen.margin))) {
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
            if (connectionState.serverConnection.isConnected()) {
                BottomSheetListItem(
                    icon = CellsIcons.Delete,
                    title = stringResource(R.string.remove_link),
                    onItemClick = { launch(NodeAction.RemoveLink) },
                )
            }
        } else if (connectionState.serverConnection.isConnected()) {
            BottomSheetFlagItem(
                nodeItem = nodeItem,
                icon = CellsIcons.ButtonShare,
                title = stringResource(R.string.public_link),
                flagType = AppNames.FLAG_SHARE,
                onItemClick = { if (it) launch(NodeAction.CreateShare) },
            )
        }
    }
}

@Composable
fun MultiNodeMenu(
    connectionState: ConnectionState,
    inRecycle: Boolean,
    containsFolders: Boolean,
    launch: (NodeAction) -> Unit,
) {
    // TODO handle case when offline
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(R.dimen.bottom_sheet_v_spacing),
                bottom = dimensionResource(R.dimen.bottom_sheet_v_spacing).times(2)
            )
            .verticalScroll(scrollState)

    ) {
        BottomSheetHeader(
            thumb = {
                M3IconThumb(
                    R.drawable.multiple_action,
                    MaterialTheme.colorScheme.onSurface
                )
            },
            title = "Choose an action",
        )
// TODO this is still broken
//        if (!containsFolders) {
//            BottomSheetListItem(
//                icon = CellsIcons.DownloadToDevice,
//                title = stringResource(R.string.download_to_device),
//                onItemClick = { launch(NodeAction.DownloadMultipleToDevice) },
//            )
//        }
        if (connectionState.serverConnection.isConnected()) {


            if (inRecycle) {
                BottomSheetListItem(
                    icon = CellsIcons.RestoreFromTrash,
                    title = stringResource(R.string.restore_content),
                    onItemClick = { launch(NodeAction.RestoreFromTrash) },
                )
                BottomSheetListItem(
                    icon = CellsIcons.DeleteForever,
                    title = stringResource(R.string.permanently_remove),
                    onItemClick = { launch(NodeAction.PermanentlyRemove) },
                )
            } else {
                BottomSheetListItem(
                    icon = CellsIcons.CopyTo,
                    title = stringResource(R.string.copy_to),
                    onItemClick = { launch(NodeAction.CopyTo) },
                )
                BottomSheetListItem(
                    icon = CellsIcons.MoveTo,
                    title = stringResource(R.string.move_to),
                    onItemClick = { launch(NodeAction.MoveTo) },
                )
                BottomSheetListItem(
                    icon = CellsIcons.Delete,
                    title = stringResource(R.string.delete),
                    onItemClick = { launch(NodeAction.Delete) },
                )
            }
        }
        BottomSheetListItem(
            icon = CellsIcons.Deselect,
            title = stringResource(R.string.deselect_all),
            onItemClick = { launch(NodeAction.UnSelectAll) }
        )
        if (!connectionState.serverConnection.isConnected()) {
            BottomSheetNoAction()
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
