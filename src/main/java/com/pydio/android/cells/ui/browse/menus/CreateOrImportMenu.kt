package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.core.composables.BottomSheetDivider
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

// private const val logTag = "CreateOrImportMenu"

@Composable
fun CreateOrImportMenu(
    stateID: StateID,
    rTreeNode: RTreeNode,
    rWorkspace: RWorkspace?,
    launch: (NodeAction) -> Unit,
    tint: Color,
    bgColor: Color,
) {
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
//        item {
//            BottomSheetHeader(
//                thumb = { Thumbnail(rTreeNode) },
//                title = stateID.fileName ?: "",
//                desc = stateID.parentPath ?: "", // TODO provide WS Label or AccountID ?
//            )
//        }
        item { BottomSheetDivider() }

        item {
            BottomSheetListItem(
                icon = CellsIcons.CreateFolder,
                title = stringResource(R.string.create_folder),
                onItemClick = { launch(NodeAction.CreateFolder) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.ImportFile,
                title = stringResource(R.string.import_files),
                onItemClick = { launch(NodeAction.ImportFile) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.TakePicture,
                title = stringResource(R.string.take_picture),
                onItemClick = { launch(NodeAction.TakePicture) },
                tint = tint,
                bgColor = bgColor,
            )
        }
    }
}

