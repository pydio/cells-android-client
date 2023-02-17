package com.pydio.android.cells.ui.browse.composables

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.NodeAction
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.browse.MoreMenuVM
import com.pydio.android.cells.ui.browse.screens.SelectFolderPage
import com.pydio.android.cells.ui.core.composables.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.BottomSheetListItem
import com.pydio.android.cells.ui.core.composables.SimpleMenuItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.utils.stateIDSaver
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private const val logTag = "NodeMoreMenu.kt"

private const val FOLDER_MAIN_CONTENT = "folder-main-content"
private const val STATE_ID_KEY = "state-id"
private const val STATE_ID_SUFFIX = "/{state-id}"
private fun route(action: NodeAction): String {
    return "${action.id}$STATE_ID_SUFFIX"
}


/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapWithActions(
    isLoading: Boolean,
    actionDone: (Boolean) -> Unit,
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
    actionDone: (Boolean) -> Unit,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    moreMenuVM: MoreMenuVM = koinViewModel(),
    content: @Composable () -> Unit,
) {

    val navController = rememberNavController()

    // We must keep a reference to the latest chosen node for contracts
    val currentID: MutableState<StateID> = rememberSaveable(stateSaver = stateIDSaver) {
        mutableStateOf(Transport.UNDEFINED_STATE_ID)
    }
    val currentAction: MutableState<String?> = rememberSaveable {
        mutableStateOf(null)
    }

    val closeDialog: (Boolean) -> Unit = {
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        if (it) {
            actionDone(true)
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            Log.e(logTag, "Got a destination for ${currentID.value}")
            if (currentID.value != Transport.UNDEFINED_STATE_ID) {
                uri?.let {
                    moreMenuVM.download(currentID.value, uri)
                }
            }
            actionDone(true)
        }
    )

    val launch: (NodeAction) -> Unit = { it ->
        if (toOpenStateID == null) {// this should never happen
            Log.e(logTag, "Trying to launch an action on a null stateID, aborting...")
        } else {
            Log.e(logTag, "About to navigate to ${it.id}/${toOpenStateID.id}")
            when (it) {
                NodeAction.DOWNLOAD_TO_DEVICE -> {
                    if (currentID.value == Transport.UNDEFINED_STATE_ID) {
                        destinationPicker.launch(toOpenStateID.fileName)
                        currentID.value = toOpenStateID
                    }
                }
                NodeAction.COPY_TO -> {
                    currentAction.value = AppNames.ACTION_COPY
                    val initialRoute =
                        "${NodeAction.SELECT_TARGET_FOLDER.id}/${toOpenStateID.parent().id}"
                    navController.navigate(initialRoute)
                }
                NodeAction.MOVE_TO -> {
                    currentAction.value = AppNames.ACTION_MOVE
                    val initialRoute =
                        "${NodeAction.SELECT_TARGET_FOLDER.id}/${toOpenStateID.parent().id}"
                    navController.navigate(initialRoute)
                }
                else -> navController.navigate("${it.id}/${toOpenStateID.id}")
            }
        }
    }

    val copyMoveAction: (StateID, String?) -> Unit = { targetStateID, action ->
        Log.e(logTag, "Do Copy/Move for $targetStateID")
        when (action) {
            AppNames.ACTION_CANCEL -> closeDialog(false)
            else -> {
                when (currentAction.value) {
                    AppNames.ACTION_MOVE -> {
                        moreMenuVM.moveTo(toOpenStateID!!, targetStateID)
                    }
                    AppNames.ACTION_COPY -> {
                        moreMenuVM.copyTo(toOpenStateID!!, targetStateID)
                    }
                }
                closeDialog(true)
            }
        }
        // Reset local copyMove Action state
        currentAction.value = null
    }

    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            Log.e(logTag, "Got a destination for ${currentID.value}")
            if (currentID.value != Transport.UNDEFINED_STATE_ID) {
                uri?.let {
                    moreMenuVM.download(currentID.value, uri)
                }
            }
        }
    )

    Scaffold { innerPadding ->
        NavHost(navController, FOLDER_MAIN_CONTENT, Modifier.padding(innerPadding)) {

            composable(FOLDER_MAIN_CONTENT) {  // Fills the area provided to the NavHost
                FolderWithMoreMenu(
                    toOpenStateID = toOpenStateID,
                    sheetState = sheetState,
                    launch = launch,
                    moreMenuVM = moreMenuVM,
                    content = content
                )
            }

            composable(route(NodeAction.SELECT_TARGET_FOLDER)) { navBackStackEntry ->
                val stateId = StateID.fromId(
                    navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                        ?: run {
                            Log.e(logTag, "... cannot navigate with no state ID")
                            return@composable
                        })

                val action = currentAction.value ?: run {
                    Log.e(logTag, "... cannot for selection with no action set")
                    return@composable
                }
                Log.e(logTag, ".... Open choose *folder* page, with ID: $stateId}")

                SelectFolderPage(
                    action = action,
                    stateID = stateId,
                    isLoading = false, // TODO
                    openFolder = {
                        val route = "${NodeAction.SELECT_TARGET_FOLDER.id}/${it.id}"
                        navController.navigate(route)
                    },
                    openParent = {
                        val route = "${NodeAction.SELECT_TARGET_FOLDER.id}/${it.parent().id}"
                        navController.navigate(route)
                    },
                    canPost = { true }, // TODO also
                    postAction = copyMoveAction,
                    forceRefresh = {/*TODO */ },
                )
            }

            dialog(route(NodeAction.RENAME)) { navBackStackEntry ->
                val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                    ?: run {
                        Log.e(logTag, "... trying to open dialog with no state ID ")
                        return@dialog
                    }
                TreeNodeRename(
                    moreMenuVM,
                    stateID = StateID.fromId(stateId),
                    dismiss = closeDialog
                )
            }

            dialog(route(NodeAction.DELETE)) { navBackStackEntry ->
                val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                    ?: run {
                        Log.e(logTag, "... trying to open dialog with no state ID ")
                        // TODO do we have to dismiss something
//                        LaunchedEffect(true) { // This should never happen anyway
//                            navController.popBackStack(BrowseDestination.AccountHome.route, false)
//                        }
                        return@dialog
                    }
                ConfirmDeletion(moreMenuVM, StateID.fromId(stateId), closeDialog)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithMoreMenu(
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    launch: (NodeAction) -> Unit,
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
    launch: (NodeAction) -> Unit,
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
                logTag,
                "## After effect, treeNode $currNode for ${currNode?.getStateID() ?: "NaN"}"
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
        Log.d(logTag, "## No more menu for $toOpenStateID")
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun NodeMoreMenuView(
    stateID: StateID,
    rTreeNode: RTreeNode,
    launch: (NodeAction) -> Unit,
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
        if (rTreeNode.isFile()) {
            item {
                BottomSheetListItem(
                    icon = CellsIcons.DownloadToDevice,
                    title = stringResource(R.string.download_to_device),
                    onItemClick = { launch(NodeAction.DOWNLOAD_TO_DEVICE) },
                    tint = tint,
                    bgColor = bgColor,
                )
            }
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Rename,
                title = stringResource(R.string.rename),
                onItemClick = { launch(NodeAction.RENAME) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.CopyTo,
                title = stringResource(R.string.copy_to),
                onItemClick = { launch(NodeAction.COPY_TO) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.MoveTo,
                title = stringResource(R.string.move_to),
                onItemClick = { launch(NodeAction.MOVE_TO) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.Link,
                title = stringResource(R.string.public_link),
                onItemClick = { launch(NodeAction.CREATE_SHARE) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.KeepOffline,
                title = stringResource(R.string.keep_offline),
                onItemClick = { launch(NodeAction.TOGGLE_OFFLINE) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            // FIXME provide a better interface for this
            BottomSheetListItem(
                icon = CellsIcons.Bookmark,
                title = stringResource(R.string.bookmark),
                onItemClick = { launch(NodeAction.TOGGLE_BOOKMARK) },
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            BottomSheetListItem(
                icon = CellsIcons.Delete,
                title = stringResource(R.string.delete),
                onItemClick = { launch(NodeAction.DELETE) },
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
