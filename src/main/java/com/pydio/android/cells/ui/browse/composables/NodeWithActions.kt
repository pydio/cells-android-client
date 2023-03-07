package com.pydio.android.cells.ui.browse.composables

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.utils.showMessage
import com.pydio.android.cells.utils.stateIDSaver
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

    object TakePicture : NodeAction("rename")
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
    loadingState: LoadingState,
    actionDone: (Boolean) -> Unit,
    type: NodeMoreMenuType,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit,
) {
    FolderWithDialogs(
//        isLoading = isLoading,
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
//    isLoading: Boolean,
    actionDone: (Boolean) -> Unit,
    type: NodeMoreMenuType,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    nodeActionsVM: NodeActionsVM = koinViewModel(),
    content: @Composable () -> Unit,
) {

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    // We must keep a reference to the latest chosen node for contracts
    val currentID: MutableState<StateID> = rememberSaveable(stateSaver = stateIDSaver) {
        mutableStateOf(StateID.NONE)
    }
    val currentAction: MutableState<String?> = rememberSaveable {
        mutableStateOf(null)
    }

    val delayedDone: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        scope.launch {
            delay(1000)
            actionDone(done)
        }
    }

    val closeDialog: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        if (done) {
            actionDone(true)
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            Log.e(logTag, "Got a destination for ${currentID.value}")
            if (currentID.value != StateID.NONE) {
                uri?.let {
                    nodeActionsVM.download(currentID.value, uri)
                }
            }
            actionDone(true)
        }
    )

    val fileImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            toOpenStateID?.let { nodeActionsVM.importFiles(it, uris) }
            actionDone(true)
        }
    )

    val photoTaker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { taken ->
            if (taken) {
                nodeActionsVM.uploadPhoto()
            } else {
                nodeActionsVM.cancelPhoto()
            }
            actionDone(taken)
        }
    )

    val context = LocalContext.current
    val copyLinkToClipboard: () -> Unit = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        toOpenStateID?.let {
            scope.launch {
                val link = nodeActionsVM.getShareLink(it)
                if (clipboard != null && link != null) {
                    val clip = ClipData.newPlainText(it.fileName, link)
                    clipboard.setPrimaryClip(clip)
                    showMessage(
                        context,
                        context.resources.getString(R.string.link_copied_to_clip)
                    )
                }
            }
        }
    }

    val launch: (NodeAction) -> Unit = { it ->
        if (toOpenStateID == null) {// this should never happen
            Log.e(logTag, "Trying to launch an action on a null stateID, aborting...")
        } else {
            Log.i(logTag, "About to navigate to ${it.id}/${toOpenStateID.id}")
            when (it) {
                is NodeAction.DownloadToDevice -> {
                    if (currentID.value == StateID.NONE) {
                        destinationPicker.launch(toOpenStateID.fileName)
                        currentID.value = toOpenStateID
                    }
                }
                is NodeAction.ImportFile -> {
                    if (currentID.value == StateID.NONE) {
                        fileImporter.launch("*/*")
                        currentID.value = toOpenStateID
                    }
                }
                is NodeAction.TakePicture -> {
                    // For this command we rather store the state in the view model
                    scope.launch {
                        nodeActionsVM.preparePhoto(context, toOpenStateID)?.also {
                            photoTaker.launch(it)
                        }
                    }
                }
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
    }

    val copyMoveAction: (String, StateID) -> Unit = { action, targetStateID ->
        Log.e(logTag, "Do Copy/Move for $targetStateID")
        when (action) {
            AppNames.ACTION_CANCEL -> closeDialog(false)
            else -> {
                when (currentAction.value) {
                    AppNames.ACTION_MOVE -> {
                        nodeActionsVM.moveTo(toOpenStateID!!, targetStateID)
                    }
                    AppNames.ACTION_COPY -> {
                        nodeActionsVM.copyTo(toOpenStateID!!, targetStateID)
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
            if (currentID.value != StateID.NONE) {
                uri?.let {
                    nodeActionsVM.download(currentID.value, uri)
                }
            }
        }
    )

    Scaffold { innerPadding ->

        NavHost(navController, FOLDER_MAIN_CONTENT, Modifier.padding(innerPadding)) {

            composable(FOLDER_MAIN_CONTENT) {  // Fills the area provided to the NavHost
                FolderWithMoreMenu(
                    type = type,
                    toOpenStateID = toOpenStateID,
                    sheetState = sheetState,
                    launch = launch,
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
                Log.e(logTag, ".... Open choose *folder* page, with ID: $stateID}")

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
                    ?: run {
                        Log.e(logTag, "... trying to open dialog with no state ID ")
                        return@dialog
                    }
                ShowQRCode(
                    nodeActionsVM,
                    stateID = StateID.fromId(stateId),
                    dismiss = { closeDialog(true) }
                )
            }

            dialog(route(NodeAction.Delete)) { navBackStackEntry ->
                val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                    ?: run {
                        Log.e(logTag, "... trying to open dialog with no state ID ")
                        // TODO do we have to dismiss something
//                        LaunchedEffect(true) { // This should never happen anyway
//                            navController.popBackStack(BrowseDestination.AccountHome.route, false)
//                        }
                        return@dialog
                    }
                ConfirmDeletion(
                    nodeActionsVM,
                    StateID.fromId(stateId)
                ) { closeDialog(it) }
            }

            dialog(route(NodeAction.PermanentlyRemove)) { navBackStackEntry ->
                val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                    ?: run { Log.e(logTag, "... Permanently remove with no ID");return@dialog }
                ConfirmPermanentDeletion(
                    nodeActionsVM,
                    StateID.fromId(stateId)
                ) { closeDialog(it) }
            }

            dialog(route(NodeAction.EmptyRecycle)) { navBackStackEntry ->
                val stateId = navBackStackEntry.arguments?.getString(STATE_ID_KEY)
                    ?: run { Log.e(logTag, "... Permanently remove with no ID");return@dialog }
                ConfirmEmptyRecycle(
                    nodeActionsVM,
                    StateID.fromId(stateId)
                ) { closeDialog(it) }
            }

            dialog(route(NodeAction.CreateFolder)) { entry ->
                val stateId = entry.arguments?.getString(STATE_ID_KEY)
                    ?: run { Log.e(logTag, "... Nav to CreateFolder, no stateID ");return@dialog }
                CreateFolder(
                    nodeActionsVM,
                    stateID = StateID.fromId(stateId),
                    dismiss = { closeDialog(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithMoreMenu(
    type: NodeMoreMenuType,
    toOpenStateID: StateID?,
    sheetState: ModalBottomSheetState,
    launch: (NodeAction) -> Unit,
    content: @Composable () -> Unit,
) {

    val tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor: Color = MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheetLayout(
        sheetContent = { NodeMoreMenuData(type, toOpenStateID, launch, tint, bgColor) },
        modifier = Modifier,
        sheetState = sheetState,
        sheetBackgroundColor = bgColor,
        content = content,
    )
}
