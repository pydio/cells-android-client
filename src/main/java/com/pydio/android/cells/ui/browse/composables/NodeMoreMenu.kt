package com.pydio.android.cells.ui.browse.composables

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.CellsActions
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.box.dialogs.CreateFolder
import com.pydio.android.cells.ui.browse.MoreMenuVM
import com.pydio.android.cells.ui.core.composables.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.SimpleMenuItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private val logTag = "NodeMoreMenu.kt"

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapWithActions(
    isLoading: Boolean,
    actionDone: () -> Unit,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit,
) {
    FolderWithDialogs(
        isLoading = isLoading,
        actionDone = actionDone,
        toOpenStateID = toOpenStateID,
        sheetState = sheetState,
        content = content
    )
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithDialogs(
    isLoading: Boolean,
    actionDone: () -> Unit,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    moreMenuVM: MoreMenuVM = koinViewModel(),
    content: @Composable () -> Unit,
) {

    val navController = rememberNavController()

    val launch: (CellsActions) -> Unit = { it ->
        navController.navigate(it.id)
    }

    Scaffold { innerPadding ->
        NavHost(navController, "content", Modifier.padding(innerPadding)) {

            composable("content") {
                // Fills the area provided to the NavHost
                FolderWithMoreMenu(
                    toOpenStateID = toOpenStateID,
                    sheetState = sheetState,
                    launch = launch,
                    moreMenuVM = moreMenuVM,
                    content = content
                )
            }

            dialog(CellsActions.DOWNLOAD_TO_DEVICE.id) {
                Log.e(logTag, "About to open dialog for DL")
                CreateFolder(
                    moreMenuVM,
                    stateID = Transport.UNDEFINED_STATE_ID,
                    dismiss = { navController.popBackStack() },
                )
            }

            dialog(CellsActions.RENAME.id) {
                Log.e(logTag, "About to open dialog for ** RENAME **")
                CreateFolder(
                    moreMenuVM,
                    stateID = Transport.UNDEFINED_STATE_ID,
                    dismiss = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithMoreMenu(
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    launch: (CellsActions) -> Unit,
    moreMenuVM: MoreMenuVM,
    content: @Composable () -> Unit,
) {

    ModalBottomSheetLayout(
        sheetContent = { NodeMoreMenuData(toOpenStateID, launch, moreMenuVM) },
        modifier = Modifier,
        sheetState = sheetState,
        content = content,
    )
}

@Composable
fun NodeMoreMenuData(
    toOpenStateID: StateID?,
    launch: (CellsActions) -> Unit,
    moreMenuVM: MoreMenuVM
) {
    val item: MutableState<RTreeNode?> = remember {
        mutableStateOf(null)
    }

    LaunchedEffect(key1 = toOpenStateID) {
        toOpenStateID?.let {
            val currNode = moreMenuVM.getTreeNode(it) ?: run {
                Log.e(logTag, "No node found for $it, aborting")
                // actionDone() TODO do something?
                null
            }
            Log.e(
                logTag, "## After effect, treeNode $currNode for ${currNode?.getStateID() ?: "NaN"}"
            )
            item.value = currNode
        }
    }

    // TODO
    //  we have to provide early a dummy content otherwise, without the check, the app crashes.
    //  And the more menu is not defined for WS roots
    if (toOpenStateID != null && toOpenStateID.parentPath != null && item.value != null) {
        val mi = item.value!!
        Log.e(logTag, "## ABOUT TO COMPOSE FOR $mi, ${mi.getStateID()}")
        NodeMoreMenuView(
            stateID = toOpenStateID,
            rTreeNode = mi,
            launch = launch,
        )
    } else {
        // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
        // when no item is defined (This is the case at the beginning when we launch the Side Effect)
        Log.e(logTag, "## No more menu for $toOpenStateID}")
        Spacer(modifier = Modifier.height(1.dp))
    }
}


@Composable
private fun NodeMoreMenuView(
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (CellsActions) -> Unit,
) {

    // TODO handle case for:
    //    recycle
    //    inside recycle
    //    when offline

    val tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor: Color = MaterialTheme.colorScheme.surfaceVariant

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            BottomSheetHeader(
                thumb = { Thumbnail(rTreeNode) },
                title = stateID.fileName ?: "",
                desc = stateID.parentPath,
            )
        }
        item {
            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.bottom_sheet_h_padding))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .6f),
                thickness = 1.dp,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.DownloadToDevice,
                title = stringResource(R.string.download_to_device),
                onItemClick = { launch(CellsActions.DOWNLOAD_TO_DEVICE) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Rename,
                title = stringResource(R.string.rename),
                onItemClick = { launch(CellsActions.RENAME) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.CopyTo,
                title = stringResource(R.string.copy_to),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.MoveTo,
                title = stringResource(R.string.move_to),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.Link,
                title = stringResource(R.string.public_link),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.KeepOffline,
                title = stringResource(R.string.keep_offline),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.Bookmark,
                title = stringResource(R.string.bookmark),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Delete,
                title = stringResource(R.string.delete),
                onItemClick = { /* TODO */ },
                tint = tint,
                bgColor = bgColor,
            )
        }

        // Still TODO add a larger bottom padding
//        item {
//            Text(" . ")
////            Spacer(
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .height(12.dp),
////            )
//        }
    }
}

@Composable
private fun CreateFolder(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: () -> Unit,

    ) {

    val doCreate: (StateID, String) -> Unit = { parentID, name ->
        moreMenuVM.createFolder(parentID, name)
        // TODO do we want a feed back?
//                if (Str.notEmpty(errMsg)) {
//                    showMessage(ctx, errMsg!!)
//                } else {
//                    browseRemoteVM.watch(parentID) // This force resets the backoff ticker
//                    showMessage(ctx, "Folder created at ${parentID.file}.")
//                }
    }

    CreateFolder(
        parStateID = stateID,
        createFolderAt = { parentId, name ->
            doCreate(parentId, name)
            dismiss()
        },
        dismiss = { dismiss() },
    )
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
        SimpleMenuItem(CellsIcons.Share, "Share") { onClick("Share") },
        SimpleMenuItem(CellsIcons.Link, "Get Link") { onClick("Get Link") },
        SimpleMenuItem(CellsIcons.Edit, "Edit") { onClick("Edit") },
        SimpleMenuItem(CellsIcons.Delete, "Delete") { onClick("Delete") },
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
