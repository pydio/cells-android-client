package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.ConnectionState
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetNoAction
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

// private const val logTag = "CreateOrImportMenu"

@Composable
fun CreateOrImportMenu(
    connectionState: ConnectionState,
    stateID: StateID,
    rTreeNode: TreeNodeItem,
    rWorkspace: RWorkspace?,
    launch: (NodeAction) -> Unit,
) {

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing))
            .verticalScroll(scrollState)

    ) {
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

        if (connectionState.serverConnection.isConnected()) {
            BottomSheetListItem(
                icon = CellsIcons.CreateFolder,
                title = stringResource(R.string.create_folder),
                onItemClick = { launch(NodeAction.CreateFolder) },
            )
            BottomSheetListItem(
                icon = CellsIcons.ImportFile,
                title = stringResource(R.string.import_files),
                onItemClick = { launch(NodeAction.ImportFile) },
            )
            BottomSheetListItem(
                icon = CellsIcons.TakePicture,
                title = stringResource(R.string.take_picture),
                onItemClick = { launch(NodeAction.TakePicture) },
            )
        } else {
            BottomSheetNoAction()
        }
    }
}
