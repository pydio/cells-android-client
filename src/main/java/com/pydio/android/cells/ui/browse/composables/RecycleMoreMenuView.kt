package com.pydio.android.cells.ui.browse.composables

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log

private const val logTag = "RecycleMoreMenuView"
@Composable
fun RecycleParentMoreMenuView(
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
                title = stringResource(id = R.string.recycle_bin_label),
                desc = stateID.parentPath,
            )
        }
        item { BottomSheetDivider() }

        item {
            BottomSheetListItem(
                icon = CellsIcons.EmptyRecycle,
                title = stringResource(R.string.empty_recycle),
                onItemClick = { launch(NodeAction.EmptyRecycle) },
                tint = tint,
                bgColor = bgColor,
            )
        }
    }
}

@Composable
fun RecycleMoreMenuView(
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (NodeAction) -> Unit,
    tint: Color,
    bgColor: Color,
) {
//     Log.e(logTag, "### encoded id for $stateID: ${stateID.id}")
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            BottomSheetHeader(
                thumb = { Thumbnail(rTreeNode) },
                title = stringResource(id = R.string.recycle_bin_label),
                desc = stateID.parentPath,
            )
        }
        item { BottomSheetDivider() }

        item {
            BottomSheetListItem(
                icon = CellsIcons.RestoreFromTrash,
                title = stringResource(R.string.restore_content),
                onItemClick = { launch(NodeAction.RestoreFromTrash) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.DeleteForever,
                title = stringResource(R.string.permanently_remove),
                onItemClick = { launch(NodeAction.PermanentlyRemove) },
                tint = tint,
                bgColor = bgColor,
            )
        }
    }
}

// FIXME
@Preview(showBackground = true)
@Composable
fun RecycleMoreMenuViewPreview() {
    val context = LocalContext.current
    val onClick: (String) -> Unit = { title ->
        Toast.makeText(
            context, title, Toast.LENGTH_SHORT
        ).show()
    }
    val stateID = StateID.fromId("admin@https%3A%2F%2Fxl-test-02.pyd.io@%2Fcommon-files%2Frecycle_bin")

//    val recycle = RTreeNode.fromFileNode(stateID, FileNode())
//
//    BottomSheetContent(
//        {
//            BottomSheetHeader(
//                icon = CellsIcons.Processing,
//                title = "My Transfer of jpg.pdf",
//                desc = "45MB, started at 5.54 AM, 46% done"
//            )
//        },
//        simpleMenuItems
//    )
}
