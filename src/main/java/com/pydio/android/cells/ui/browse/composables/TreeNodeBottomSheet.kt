package com.pydio.android.cells.ui.browse.composables

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.TreeNodeVM
import com.pydio.android.cells.ui.core.composables.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.SimpleMenuItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsVectorIcons
import com.pydio.cells.transport.StateID

@Composable
fun TreeNodeBottomSheet(
    treeNodeVM: TreeNodeVM,
    item: RTreeNode?,
    onClick: (String, StateID) -> Unit
) {
    if (item == null) {
        // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
        // when no item is defined (default case at starting point)
        Spacer(modifier = Modifier.height(1.dp))
        return
    }

    val context = LocalContext.current

    // Create the menu
    val simpleMenuItems: List<SimpleMenuItem> =
        getFileMenuItems(node = item, onClick = onClick)

    BottomSheetContent({
        BottomSheetHeader(
            thumb = { Thumbnail(item) },
            title = item.getStateID().fileName,
            desc = item.parentPath,
        )
    }, simpleMenuItems)
}

@Composable
private fun getFileMenuItems(
    node: RTreeNode,
    onClick: (String, StateID) -> Unit,
): List<SimpleMenuItem> {

    val menuItems: MutableList<SimpleMenuItem> = mutableListOf()
    menuItems.add(
        SimpleMenuItem(
            CellsVectorIcons.Pause,
            stringResource(id = R.string.pause)
        ) { onClick(AppNames.ACTION_CANCEL, node.getStateID()) },
    )
    menuItems.add(
        SimpleMenuItem(
            CellsVectorIcons.Pause,
            stringResource(id = R.string.pause)
        ) { onClick(AppNames.ACTION_CANCEL, node.getStateID()) },
    )
    menuItems.add(
        SimpleMenuItem(
            CellsVectorIcons.Pause,
            stringResource(id = R.string.pause)
        ) { onClick(AppNames.ACTION_CANCEL, node.getStateID()) },
    )
    menuItems.add(
        SimpleMenuItem(
            CellsVectorIcons.Pause,
            stringResource(id = R.string.pause)
        ) { onClick(AppNames.ACTION_CANCEL, node.getStateID()) },
    )

    return menuItems
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
        SimpleMenuItem(CellsVectorIcons.Share, "Share") { onClick("Share") },
        SimpleMenuItem(CellsVectorIcons.Link, "Get Link") { onClick("Get Link") },
        SimpleMenuItem(CellsVectorIcons.Edit, "Edit") { onClick("Edit") },
        SimpleMenuItem(CellsVectorIcons.Delete, "Delete") { onClick("Delete") },
    )

    BottomSheetContent(
        {
            BottomSheetHeader(
                icon = CellsVectorIcons.Processing,
                title = "My Transfer of jpg.pdf",
                desc = "45MB, started at 5.54 AM, 46% done"
            )
        },
        simpleMenuItems
    )
}
