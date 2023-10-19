package com.pydio.android.cells.ui.browse.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.toErrorMessage
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val LOG_TAG = "NodeWithActions.kt"
private const val FOLDER_MAIN_CONTENT = "folder-main-content"

private fun route(action: NodeAction): String {
    return "${action.id}/{${AppKeys.STATE_ID}}"
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapWithActions(
    actionDone: (Boolean) -> Unit,
    type: NodeMoreMenuType,
    targetStateIDs: Set<StateID>,
    sheetState: ModalBottomSheetState,
    snackBarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    FolderWithDialogs(
        actionDone = actionDone,
        type = type,
        targetStateIDs = targetStateIDs,
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
    targetStateIDs: Set<StateID>,
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
        if (clipboard != null && targetStateIDs.size == 1) {
            val clip = ClipData.newPlainText(targetStateIDs.first().fileName, link)
            clipboard.setPrimaryClip(clip)
            showMessage(
                context,
                context.resources.getString(R.string.link_copied_to_clip)
            )
        }
    }

    val launchMulti: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
        if (stateIDs.size < 2) {
            Log.e(LOG_TAG, "Cannot launch $action without at least 2 items ")
            Log.d(LOG_TAG, "Currently we have only ${stateIDs.size}.")
        } else when (action) {
            is NodeAction.CopyTo -> {
                currentAction.value = AppNames.ACTION_COPY
                // FIXME double check and fix.
                //  was encodeStateForRoute(passedStateID.parent())
                val suffix = encodeStateForRoute(stateIDs.first().parent())
                val initialRoute = "${NodeAction.SelectTargetFolder.id}/$suffix"
                navController.navigate(initialRoute)
            }

            is NodeAction.MoveTo -> {
                currentAction.value = AppNames.ACTION_MOVE
                val suffix = encodeStateForRoute(stateIDs.first().parent())
                val initialRoute = "${NodeAction.SelectTargetFolder.id}/$suffix"
                navController.navigate(initialRoute)
            }

            is NodeAction.Delete -> {
                for (stateID in stateIDs) {
                    nodeActionsVM.delete(stateID)
                }
                actionDone(true)
            }

//            is NodeAction.DownloadToDevice -> {
//                Log.e(LOG_TAG, "Implement me: $action for multi")
//            }

            is NodeAction.RestoreFromTrash -> {
                for (stateID in stateIDs) {
                    nodeActionsVM.restoreFromTrash(stateID)
                }
                actionDone(true)
            }

            is NodeAction.PermanentlyRemove -> {
                for (stateID in stateIDs) {
                    nodeActionsVM.delete(stateID)
                }
                actionDone(true)
            }

            else -> {
                Log.e(LOG_TAG, "unexpected action: $action")
            }
        }
    }

    val launchMono: (NodeAction, StateID) -> Unit = { action, passedStateID ->
        Log.i(LOG_TAG, "Launching mono action for ${action.id}/${passedStateID}")
        when (action) {
            is NodeAction.CopyTo -> {
                currentAction.value = AppNames.ACTION_COPY
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${encodeStateForRoute(passedStateID.parent())}"
                navController.navigate(initialRoute)
            }

            is NodeAction.MoveTo -> {
                currentAction.value = AppNames.ACTION_MOVE
                val initialRoute =
                    "${NodeAction.SelectTargetFolder.id}/${encodeStateForRoute(passedStateID.parent())}"
                navController.navigate(initialRoute)
            }

            is NodeAction.ToggleOffline -> {
                nodeActionsVM.toggleOffline(passedStateID, action.isChecked)
                delayedDone(true)
            }

            is NodeAction.ToggleBookmark -> {
                nodeActionsVM.toggleBookmark(passedStateID, action.isChecked)
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
                    nodeActionsVM.getShareLink(passedStateID)?.let {
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

            else -> navController.navigate("${action.id}/${encodeStateForRoute(passedStateID)}")
        }
    }

//    val launch: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
//        if (stateIDs.size == 1) {
//            launchMono(action, stateIDs.first())
//        } else {
//            launchMulti(action, stateIDs)
//        }
//    }

    val copyMoveAction: (String, StateID) -> Unit = { action, targetStateID ->
        Log.i(LOG_TAG, "launch $action action for $targetStateID")
        when (action) {
            AppNames.ACTION_CANCEL -> closeDialog(false)
            else -> {
                for (currID in targetStateIDs) {
                    when (currentAction.value) {
                        AppNames.ACTION_MOVE -> {
                            nodeActionsVM.moveTo(currID, targetStateID)
                        }

                        AppNames.ACTION_COPY -> {
                            nodeActionsVM.copyTo(currID, targetStateID)
                        }
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
                sheetContent = {
                    if (targetStateIDs.size == 1) {
                        NodeMoreMenuData(
                            type = type,
                            toOpenStateID = targetStateIDs.first(),
                            launch = launchMono,
                        )
                    } else if (targetStateIDs.size > 1) {
                        NodesMoreMenuData(
                            type = type,
                            stateIDs = targetStateIDs,
                            launch = launchMulti,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                },
                sheetState = sheetState,
                content = content
            )
        }

        composable(route(NodeAction.SelectTargetFolder)) { nbsEntry ->
            val stateID = lazyStateID(nbsEntry)
            if (stateID == StateID.NONE) {
                Log.e(LOG_TAG, "... cannot navigate with no state ID")
                return@composable
            }
            val action = currentAction.value ?: run {
                Log.e(LOG_TAG, "... cannot launch target selection with no action set")
                return@composable
            }
            LaunchedEffect(key1 = stateID) {
                Log.i(LOG_TAG,"## First Composition for: selectTarget/$stateID")
            }
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
                Log.e(LOG_TAG, "... cannot navigate with no state ID")
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
                Log.w(LOG_TAG, "... ShowQRCode with no ID ")
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
                Log.w(LOG_TAG, "... Delete with no ID ")
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
                Log.w(LOG_TAG, "... PermanentlyRemove with no ID")
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
                Log.w(LOG_TAG, "... EmptyRecycle with no ID")
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
                Log.w(LOG_TAG, "... CreateFolder with no ID")
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
            if (stateID == StateID.NONE) {
                Log.w(LOG_TAG, "... CreateFolder with no ID")
                return@dialog
            }
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
