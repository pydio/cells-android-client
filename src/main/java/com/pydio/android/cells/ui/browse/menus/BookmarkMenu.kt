package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.android.cells.ui.core.composables.DefaultTitleText
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetListItem
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

// private const val logTag = "BookmarkMenu"

@Composable
fun BookmarkMenu(
    treeNodeVM: TreeNodeVM,
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (NodeAction, StateID) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing))
            .verticalScroll(scrollState)

    ) {
        BottomSheetHeader(
            thumb = { Thumbnail(rTreeNode) },
            title = stateID.fileName ?: "",
            desc = stateID.parentPath,
        )
        BottomSheetListItem(
            icon = CellsIcons.Bookmark,
            title = stringResource(R.string.remove_bookmark),
            onItemClick = { launch(NodeAction.ToggleBookmark(false), stateID) }
        )
        if (rTreeNode.isFile()) {
            BottomSheetListItem(
                icon = CellsIcons.DownloadToDevice,
                title = stringResource(R.string.download_to_device),
                onItemClick = { launch(NodeAction.DownloadToDevice, stateID) },
            )
        }

        val appearsIn = remember { mutableStateOf<MultipleItem?>(null) }

        appearsIn.value?.let {
            DefaultTitleText(
                text = if (rTreeNode.isFile()) {
                    stringResource(R.string.open_parent_in_workspaces)
                } else {
                    stringResource(R.string.open_in_workspaces)
                },
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.bottom_sheet_start_padding),
                    end = dimensionResource(R.dimen.bottom_sheet_start_padding),
                    top = dimensionResource(R.dimen.bottom_sheet_v_spacing),
                    bottom = dimensionResource(R.dimen.bottom_sheet_v_padding),
                ),
            )
            // Handle multiple path
            for (bi in it.appearsIn) {
                BottomSheetListItem(
                    icon = CellsIcons.OpenLocation,
                    title = bi.parent().path,
                    onItemClick = { launch(NodeAction.OpenInApp, bi) },
                )
            }
        }
        LaunchedEffect(key1 = stateID) {
            appearsIn.value = treeNodeVM.appearsIn(stateID)
        }
    }
}
