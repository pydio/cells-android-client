package com.pydio.android.cells.ui.browse.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.toErrorMessage
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "NodeWithActions.kt"
private const val FOLDER_MAIN_CONTENT = "folder-main-content"

private fun route(action: NodeAction): String {
    return "${action.id}/{${AppKeys.STATE_ID}}"
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

    object OpenParentLocation : NodeAction("open_parent_location")

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
    snackBarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    FolderWithDialogs(
        actionDone = actionDone,
        type = type,
        toOpenStateID = toOpenStateID,
        sheetState = sheetState,
        snackBarHostState = snackBarHostState,
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
    snackBarHostState: SnackbarHostState,
    nodeActionsVM: NodeActionsVM = koinViewModel(),
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
    content: @Composable () -> Unit,
) {

    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val currentAction: MutableState<String?> = rememberSaveable {
        mutableStateOf(null)
    }

    val errMsg = nodeActionsVM.errorMessage.collectAsState(null)

    errMsg.value?.let {
        LaunchedEffect(key1 = it) {
            // TODO finalise this
            snackBarHostState.showSnackbar(
                message = toErrorMessage(context, it),
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
//            val snackBarResult = snackBarHostState.showSnackbar(
//                message = it,
//                withDismissAction = false,
//                duration = SnackbarDuration.Short
//            )
//            when (snackBarResult) {
//                SnackbarResult.ActionPerformed -> {
//                    Log.e("Snackbar", "Action Performed")
//                }
//
//                else -> {
//                    Log.e("Snackbar", "Snackbar dismissed")
//                }
//            }
        }
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

    val copyLinkToClipboard: (String) -> Unit = { link ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null) {
            val clip = ClipData.newPlainText(toOpenStateID.fileName, link)
            clipboard.setPrimaryClip(clip)
            showMessage(
                context,
                context.resources.getString(R.string.link_copied_to_clip)
            )
        }
    }

    val launch: (NodeAction, StateID) -> Unit = { it, passedStateID ->
        Log.i(logTag, "About to navigate to ${it.id}/${passedStateID}")
        when (it) {
            is NodeAction.CopyTo -> {
                currentAction.value = AppNames.ACTION_COPY
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${passedStateID.parent().id}"
                navController.navigate(initialRoute)
            }

            is NodeAction.MoveTo -> {
                currentAction.value = AppNames.ACTION_MOVE
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${passedStateID.parent().id}"
                navController.navigate(initialRoute)
            }

            is NodeAction.ToggleOffline -> {
                nodeActionsVM.toggleOffline(passedStateID, it.isChecked)
                delayedDone(true)
            }

            is NodeAction.ToggleBookmark -> {
                nodeActionsVM.toggleBookmark(passedStateID, it.isChecked)
                delayedDone(true)
            }

            is NodeAction.CreateShare -> {
                scope.launch {
                    nodeActionsVM.createShare(passedStateID)?.let {
                        copyLinkToClipboard(it)
                    }
                    actionDone(true)
                }
            }

            is NodeAction.ShareWith -> {
                scope.launch {
                    nodeActionsVM.getShareLink(passedStateID)?.let {
                        context.startActivity(
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, it)
                        )
                    }
                    actionDone(true)
                }
            }

            is NodeAction.CopyToClipboard -> {
                scope.launch {
                    nodeActionsVM.getShareLink(toOpenStateID)?.let {
                        copyLinkToClipboard(it)
                    }
                    actionDone(true)
                }
            }

            is NodeAction.RemoveLink -> {
                nodeActionsVM.removeShare(passedStateID)
                actionDone(true)
            }

            is NodeAction.RestoreFromTrash -> {
                nodeActionsVM.restoreFromTrash(passedStateID)
                actionDone(true)
            }

            is NodeAction.SortBy -> {
                actionDone(true)
            }

            else -> navController.navigate("${it.id}/${passedStateID.id}")
        }
    }

    val copyMoveAction: (String, StateID) -> Unit = { action, targetStateID ->
        Log.i(logTag, "launch $action action for $targetStateID")
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
            // Log.e(logTag, "... Composing action NavHost with stateID: $toOpenStateID")
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
                Log.e(logTag, "... cannot launch target selection with no action set")
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

            DisposableEffect(key1 = stateID) {
                if (stateID == StateID.NONE) {
                    browseRemoteVM.pause(StateID.NONE)
                } else {
                    browseRemoteVM.watch(stateID, false)
                }
                onDispose {
                    browseRemoteVM.pause(stateID)
                }
            }
        }

        dialog(route(NodeAction.Rename)) { entry ->
            val stateID = lazyStateID(entry)
            if (stateID == StateID.NONE) {
                Log.e(logTag, "... cannot navigate with no state ID")
                return@dialog
            }
            TreeNodeRename(
                nodeActionsVM,
                stateID = stateID,
                dismiss = { closeDialog(it) }
            )
        }

        dialog(route(NodeAction.ShowQRCode)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "... ShowQRCode with no ID ")
                return@dialog
            }
            ShowQRCode(
                nodeActionsVM,
                stateID = currID,
                dismiss = { closeDialog(true) }
            )
        }

        dialog(route(NodeAction.Delete)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "... Delete with no ID ")
                return@dialog
            }
            ConfirmDeletion(
                nodeActionsVM,
                currID
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.PermanentlyRemove)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "... PermanentlyRemove with no ID")
                return@dialog
            }
            ConfirmPermanentDeletion(
                nodeActionsVM,
                currID
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.EmptyRecycle)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "... EmptyRecycle with no ID")
                return@dialog
            }
            ConfirmEmptyRecycle(
                nodeActionsVM,
                currID
            ) { closeDialog(it) }
        }

        dialog(route(NodeAction.CreateFolder)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "... CreateFolder with no ID")
                return@dialog
            }
            CreateFolder(
                nodeActionsVM,
                stateID = currID,
                dismiss = { closeDialog(it) }
            )
        }

        dialog(route(NodeAction.DownloadToDevice)) { entry ->
            val stateID = lazyStateID(entry)
            ChooseDestination(
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
