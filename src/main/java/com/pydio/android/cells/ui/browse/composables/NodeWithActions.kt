package com.pydio.android.cells.ui.browse.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.pydio.android.cells.services.ConnectionState
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.android.cells.ui.core.encodeStateSetForRoute
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.lazyStateIDs
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.toErrorMessage
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.share.screens.SelectFolderScreen
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val LOG_TAG = "NodeWithActions.kt"
private const val FOLDER_MAIN_CONTENT = "folder-main-content"

private fun route(action: NodeAction): String {
    return "${action.id}/{${AppKeys.STATE_ID}}"
}

private fun routeMulti(action: NodeAction): String {
    return "${action.id}/{${AppKeys.STATE_IDS}}"
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapWithActions(
    actionDone: (Boolean, Boolean) -> Unit,
    isExpandedScreen: Boolean = false,
    connectionState: ConnectionState,
    type: NodeMoreMenuType,
    subjectIDs: Set<StateID>,
    sheetState: ModalBottomSheetState,
    snackBarHostState: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(key1 = subjectIDs.toString()) {
        Log.d(LOG_TAG, "## Recomposing Wrap with actions for $subjectIDs")
    }

    FolderWithDialogs(
        actionDone = actionDone,
        isExpandedScreen = isExpandedScreen,
        connectionState = connectionState,
        type = type,
        subjectIDs = subjectIDs,
        sheetState = sheetState,
        snackBarHostState = snackBarHostState,
        content = content
    )
}

/** Add the more menu **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithDialogs(
    actionDone: (Boolean, Boolean) -> Unit,
    isExpandedScreen: Boolean,
    connectionState: ConnectionState,
    type: NodeMoreMenuType,
    subjectIDs: Set<StateID>,
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
            val snackBarResult = snackBarHostState.showSnackbar(
                message = toErrorMessage(context, it),
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            when (snackBarResult) {
                SnackbarResult.ActionPerformed -> {
                    Log.e(LOG_TAG, "Action Performed for err: $it")
                }

                else -> {
                    Log.e(LOG_TAG, "Snack-bar dismissed for err: $it")
                }
            }
        }
    }

    // This introduce a small delay before closing the menu to e.G let the end-user see the toggle animation before closing the bottom menu
    val delayedDone: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        scope.launch {
            delay(400)
            actionDone(done, true)
        }
    }

    val closeDialog: (Boolean, Boolean) -> Unit = { done, doRefresh ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        if (done) {
            actionDone(true, doRefresh)
        }
    }

    val launchMulti: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
        if (stateIDs.size < 2) {
            Log.e(LOG_TAG, "Cannot launch $action without at least 2 items ")
            Log.d(LOG_TAG, "Currently we have only ${stateIDs.size}.")
        } else when (action) {

            is NodeAction.CopyTo -> {
                currentAction.value = AppNames.ACTION_COPY
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

            // TODO still not working
            is NodeAction.DownloadMultipleToDevice -> {
                val suffix = encodeStateSetForRoute(stateIDs)
                val initialRoute = "${NodeAction.DownloadMultipleToDevice.id}/$suffix"
                navController.navigate(initialRoute)
            }

            is NodeAction.Delete, NodeAction.PermanentlyRemove -> {
                nodeActionsVM.delete(stateIDs)
                delayedDone(true)
            }

            is NodeAction.RestoreFromTrash -> {
                nodeActionsVM.restoreFromTrash(stateIDs)
                delayedDone(true)
            }

            is NodeAction.UnSelectAll -> {
                delayedDone(true)
            }

            else -> {
                Log.e(LOG_TAG, "unexpected action: $action")
            }
        }
    }

    val copyLinkToClipboard: (String) -> Unit = { link ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard != null && subjectIDs.size == 1) {
            val clip = ClipData.newPlainText(subjectIDs.first().fileName, link)
            clipboard.setPrimaryClip(clip)
            showMessage(
                context,
                context.resources.getString(R.string.link_copied_to_clip)
            )
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
                // We use delay done so that we see the toggle animation before closing the bottom menu
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
                    actionDone(true, true)
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
                    actionDone(true, false)
                }
            }

            is NodeAction.CopyToClipboard -> {
                scope.launch {
                    nodeActionsVM.getShareLink(passedStateID)?.let {
                        copyLinkToClipboard(it)
                    }
                    actionDone(true, false)
                }
            }

            is NodeAction.RemoveLink -> {
                nodeActionsVM.removeShare(passedStateID)
                actionDone(true, true)
            }

            is NodeAction.RestoreFromTrash -> {
                nodeActionsVM.restoreFromTrash(passedStateID)
                actionDone(true, true)
            }

            is NodeAction.SortBy -> {
                actionDone(true, false)
            }

            else -> navController.navigate("${action.id}/${encodeStateForRoute(passedStateID)}")
        }
    }

    val copyMoveAction: (String, StateID) -> Unit = { action, targetStateID ->
        Log.i(LOG_TAG, "... Launching $action action for $targetStateID")
        when (action) {
            AppNames.ACTION_CANCEL -> {
                closeDialog(false, false)
                currentAction.value = null
            }

            else -> {
                scope.launch {
                    for (currID in subjectIDs) {
                        when (currentAction.value) {
                            AppNames.ACTION_MOVE -> {
                                nodeActionsVM.moveTo(currID, targetStateID)
                            }

                            AppNames.ACTION_COPY -> {
                                nodeActionsVM.copyTo(currID, targetStateID)
                            }
                        }
                    }
                }
                closeDialog(true, true)
            }
        }
    }

    NavHost(navController, FOLDER_MAIN_CONTENT) {

        composable(FOLDER_MAIN_CONTENT) {  // Fills the area provided to the NavHost
            CellsModalBottomSheetLayout(
                isExpandedScreen = isExpandedScreen,

                sheetContent = {
                    if (subjectIDs.size == 1) {
                        NodeMoreMenuData(
                            connectionState = connectionState,
                            type = type,
                            subjectID = subjectIDs.first(),
                            launch = launchMono,
                        )
                    } else if (subjectIDs.size > 1) {
                        NodesMoreMenuData(
                            connectionState = connectionState,
                            type = type,
                            subjectIDs = subjectIDs,
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
            val targetAction = currentAction.value ?: run {
                Log.e(LOG_TAG, "... cannot launch target selection with no action set")
                return@composable
            }
            LaunchedEffect(key1 = stateID) {
                Log.i(LOG_TAG, "## First Composition for: selectTarget/$stateID")
            }
            val shareVM: ShareVM = koinViewModel { parametersOf(stateID) }
            SelectFolderScreen(
                targetAction = targetAction,
                stateID = stateID,
                subjects = subjectIDs,
                browseRemoteVM = browseRemoteVM,
                shareVM = shareVM,
                open = {
                    val route = "${NodeAction.SelectTargetFolder.id}/${encodeStateForRoute(it)}"
                    navController.navigate(route)
                },
                canPost = { // We rather rely on the non-click-ability of forbidden targets
                    if (targetAction == AppNames.ACTION_MOVE) {
                        // Prevent from moving in the same folder
                        subjectIDs.first().parent() != stateID
                    } else {
                        true
                    }
                },
                doAction = copyMoveAction,
            )
            DisposableEffect(key1 = stateID) {
                if (stateID == StateID.NONE) {
                    browseRemoteVM.pause(StateID.NONE)
                } else {
                    browseRemoteVM.watch(stateID, false)
                }
                onDispose {
                    if (subjectIDs.first().parent() == stateID) { // Do nothing
                        // Corner case when we copy move in the same folder: do not stop the polling
                        Log.e(LOG_TAG, "... On dispose for $stateID, WITHOUT STOPPING THE POLL")
                    } else {
                        browseRemoteVM.pause(stateID)
                        Log.e(LOG_TAG, "... Disposing select folder page for $stateID")
                    }
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
                dismiss = { closeDialog(it, it) }
            )
        }

        dialog(route(NodeAction.ShowQRCode)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(LOG_TAG, "... ShowQRCode with no ID - Abort")
                return@dialog
            }
            ShowQRCode(
                nodeActionsVM,
                stateID = currID,
                dismiss = { closeDialog(true, false) }
            )
        }

        dialog(route(NodeAction.Delete)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(LOG_TAG, "... Delete with no ID - Abort")
                return@dialog
            }
            ConfirmDeletion(
                nodeActionsVM,
                currID
            ) { closeDialog(it, it) }
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
            ) { closeDialog(it, it) }
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
            ) { closeDialog(it, it) }
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
                dismiss = { closeDialog(it, it) }
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
                dismiss = { closeDialog(it, it) }
            )
        }

        dialog(routeMulti(NodeAction.DownloadMultipleToDevice)) { entry ->
            val stateIDs = lazyStateIDs(entry)
            if (stateIDs.isEmpty() || (stateIDs.size == 1 && stateIDs.first() == StateID.NONE)) {
                Log.w(LOG_TAG, "... CreateFolder with no ID")
                return@dialog
            }
            ChooseFolderDestination(
                nodeActionsVM,
                stateIDs = stateIDs,
                dismiss = { closeDialog(it, it) }
            )
        }

        dialog(route(NodeAction.ImportFile)) { entry ->
            val stateID = lazyStateID(entry)
            ImportFile(
                nodeActionsVM,
                targetParentID = stateID,
                dismiss = { closeDialog(it, it) }
            )
        }

        dialog(route(NodeAction.TakePicture)) { entry ->
            val stateID = lazyStateID(entry)
            TakePicture(
                nodeActionsVM,
                targetParentID = stateID,
                dismiss = {
                    Log.e(LOG_TAG, "After TakePicture done = $it")
                    if (it) { // We must use a delay here or the event is missed and the scrim is not discarded
                        delayedDone(true)
                    } else {
                        closeDialog(false, false)
                    }
                }
            )
        }
    }
}
