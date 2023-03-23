package com.pydio.android.cells.ui.browse.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "NodeWithActions.kt"

private const val FOLDER_MAIN_CONTENT = "folder-main-content"
private const val STATE_ID_KEY = "state-id"
private const val STATE_ID_SUFFIX = "/{state-id}"

private fun route(action: NodeAction): String {
    return "${action.id}$STATE_ID_SUFFIX"
}

sealed class NodeAction(val id: String) {
    object DownloadToDevice : NodeAction("download_to_device")
    object Rename : NodeAction("rename")
    object CopyTo : NodeAction("copy_to")
    object MoveTo : NodeAction("move_to")
    object CreateShare : NodeAction("create_share")
    object ShareWith : NodeAction("share_with")
    object CopyToClipboard : NodeAction("copy_to_Clipboard")
    object ShowQRCode : NodeAction("show_qr_code")
    object RemoveLink : NodeAction("remove_link")

    object TakePicture : NodeAction("take_picture")
    object ImportFile : NodeAction("import_file")
    object CreateFolder : NodeAction("create_folder")

    object ManageShare : NodeAction("manage_share")
    class ToggleOffline(val isChecked: Boolean) : NodeAction("toggle_offline")
    class ToggleBookmark(val isChecked: Boolean) : NodeAction("toggle_bookmark")
    object Delete : NodeAction("delete")

    object RestoreFromTrash : NodeAction("restore_from_trash")
    object PermanentlyRemove : NodeAction("permanently_remove")
    object EmptyRecycle : NodeAction("empty_recycle")
    object SelectTargetFolder : NodeAction("select_target_folder")

    object ForceResync : NodeAction("force_re_sync")
    object OpenInApp : NodeAction("open_in_app")

    object SortBy : NodeAction("sort_by")
    object AsList : NodeAction("as_list")
    object AsGrid : NodeAction("as_grid")
    object AsSmallerGrid : NodeAction("as_smaller_grid")
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapWithActions(
    actionDone: (Boolean) -> Unit,
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit,
) {
    FolderWithDialogs(
        actionDone = actionDone,
        type = type,
        toOpenStateID = toOpenStateID,
        sheetState = sheetState,
        content = content
    )
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithDialogs(
    actionDone: (Boolean) -> Unit,
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    sheetState: ModalBottomSheetState,
    nodeActionsVM: NodeActionsVM = koinViewModel(),
    content: @Composable () -> Unit,
) {

    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val currentAction: MutableState<String?> = rememberSaveable {
        mutableStateOf(null)
    }

    val delayedDone: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        scope.launch {
            delay(200)
            actionDone(done)
        }
    }

    val closeDialog: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        if (done) {
            actionDone(true)
        }
    }

    val copyLinkToClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        scope.launch {
            val link = nodeActionsVM.getShareLink(toOpenStateID)
            if (clipboard != null && link != null) {
                val clip = ClipData.newPlainText(toOpenStateID.fileName, link)
                clipboard.setPrimaryClip(clip)
                showMessage(
                    context,
                    context.resources.getString(R.string.link_copied_to_clip)
                )
            }
        }
    }

    val launch: (NodeAction) -> Unit = { it ->
        Log.i(logTag, "About to navigate to ${it.id}/${toOpenStateID}")
        when (it) {
            is NodeAction.CopyTo -> {
                currentAction.value = AppNames.ACTION_COPY
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${toOpenStateID.parent().id}"
                navController.navigate(initialRoute)
            }
            is NodeAction.MoveTo -> {
                currentAction.value = AppNames.ACTION_MOVE
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${toOpenStateID.parent().id}"
                navController.navigate(initialRoute)
            }
            is NodeAction.ToggleOffline -> {
                nodeActionsVM.toggleOffline(toOpenStateID, it.isChecked)
                delayedDone(true)
            }
            is NodeAction.ToggleBookmark -> {
                nodeActionsVM.toggleBookmark(toOpenStateID, it.isChecked)
                delayedDone(true)
            }
            is NodeAction.CreateShare -> {
                nodeActionsVM.createShare(toOpenStateID)
                copyLinkToClipboard()
                delayedDone(true)
            }
            is NodeAction.ShareWith -> {
                nodeActionsVM.createShare(toOpenStateID)
                actionDone(true)
            }
            is NodeAction.CopyToClipboard -> {
                copyLinkToClipboard()
                actionDone(true)
            }
            is NodeAction.RemoveLink -> {
                nodeActionsVM.removeShare(toOpenStateID)
                actionDone(true)
            }
            is NodeAction.RestoreFromTrash -> {
                nodeActionsVM.restoreFromTrash(toOpenStateID)
                actionDone(true)
            }
            is NodeAction.SortBy -> {
                actionDone(true)
            }
            else -> navController.navigate("${it.id}/${toOpenStateID.id}")
        }
    }

    val copyMoveAction: (String, StateID) -> Unit = { action, targetStateID ->
        Log.e(logTag, "Do Copy/Move for $targetStateID")
        when (action) {
            AppNames.ACTION_CANCEL -> closeDialog(false)
            else -> {
                when (currentAction.value) {
                    AppNames.ACTION_MOVE -> {
                        nodeActionsVM.moveTo(toOpenStateID, targetStateID)
                    }
                    AppNames.ACTION_COPY -> {
                        nodeActionsVM.copyTo(toOpenStateID, targetStateID)
                    }
                }
                closeDialog(true)
            }
        }
        // Reset local copyMove Action state
        currentAction.value = null
    }

    NavHost(navController, FOLDER_MAIN_CONTENT) {

        composable(FOLDER_MAIN_CONTENT) {  // Fills the area provided to the NavHost
            CellsModalBottomSheetLayout(
                sheetContent = { NodeMoreMenuData(type, toOpenStateID, launch) },
                sheetState = sheetState,
                content = content
            )
        }

        composable(route(NodeAction.SelectTargetFolder)) { nbsEntry ->
            val stateID = lazyStateID(nbsEntry)
            if (stateID == StateID.NONE) {
                Log.e(logTag, "... cannot navigate with no state ID")
                return@composable
            }

            val action = currentAction.value ?: run {
                Log.e(logTag, "... cannot for selection with no action set")
                return@composable
            }
            Log.i(logTag, ".... Open choose *folder* page, with ID: $stateID}")
            val folderVM: FolderVM = koinViewModel(parameters = { parametersOf(stateID) })

            SelectFolderPage(
                action = action,
                stateID = stateID,
                loadingStatus = LoadingState.IDLE, // FIXME
                forceRefresh = {/*TODO */ },
                open = {
                    // TODO rather use route function
                    val route = "${NodeAction.SelectTargetFolder.id}/${it.id}"
                    navController.navigate(route)
                },
                canPost = { true }, // TODO also
                doAction = copyMoveAction,
                folderVM = folderVM,
            )
        }

        dialog(route(NodeAction.Rename)) { entry ->
            val stateId = entry.arguments?.getString(STATE_ID_KEY)
                ?: run {
                    Log.e(logTag, "... trying to open dialog with no state ID ")
                    return@dialog
                }
            TreeNodeRename(
                nodeActionsVM,
                stateID = StateID.fromId(stateId),
                dismiss = { closeDialog(it) }
            )
        }

        dialog(route(NodeAction.ShowQRCode)) { entry ->
            val stateId = entry.arguments?.getString(STATE_ID_KEY)
                ?: run { Log.e(logTag, "... ShowQRCode with no ID "); return@dialog }
            ShowQRCode(
                nodeActionsVM,
                stateID = StateID.fromId(stateId),
                dismiss = { closeDialog(true) }
            )
        }

        dialog(route(NodeAction.Delete)) { navBackStackEntry ->
            val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                ?: run { Log.e(logTag, "... Delete with no ID "); return@dialog }
            ConfirmDeletion(
                nodeActionsVM,
                StateID.fromId(stateId)
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.PermanentlyRemove)) { navBackStackEntry ->
            val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                ?: run { Log.w(logTag, "... PermanentlyRemove with no ID"); return@dialog }
            ConfirmPermanentDeletion(
                nodeActionsVM,
                StateID.fromId(stateId)
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.EmptyRecycle)) { navBackStackEntry ->
            val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                ?: run { Log.e(logTag, "... EmptyRecycle with no ID"); return@dialog }
            ConfirmEmptyRecycle(
                nodeActionsVM,
                StateID.fromId(stateId)
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.CreateFolder)) { entry ->
            val stateId = entry.arguments?.getString(STATE_ID_KEY)
                ?: run { Log.e(logTag, "... CreateFolder with no ID"); return@dialog }
            CreateFolder(
                nodeActionsVM,
                stateID = StateID.fromId(stateId),
                dismiss = { closeDialog(it) }
            )
        }

        dialog(route(NodeAction.DownloadToDevice)) { entry ->
            val stateID = lazyStateID(entry)
            PickDestination(
                nodeActionsVM,
                stateID = stateID,
                dismiss = { closeDialog(it) }
            )
        }

        dialog(route(NodeAction.ImportFile)) { entry ->
            val stateID = lazyStateID(entry)
            ImportFile(
                nodeActionsVM,
                targetParentID = stateID,
                dismiss = { closeDialog(it) }
            )
        }
        dialog(route(NodeAction.TakePicture)) { entry ->
            val stateID = lazyStateID(entry)
            TakePicture(
                nodeActionsVM,
                targetParentID = stateID,
                dismiss = { closeDialog(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithMoreMenu(
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    sheetState: ModalBottomSheetState,
    launch: (NodeAction) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetContent = { NodeMoreMenuData(type, toOpenStateID, launch) },
        modifier = Modifier,
        sheetState = sheetState,
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}
