package com.pydio.android.cells.ui.browse.composables

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.MoreMenuVM
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.core.composables.dialogs.AskForFolderName
import com.pydio.android.cells.ui.core.composables.dialogs.AskForNewName
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

private val logTag = "NodeActionDialogs.kt"

@Composable
fun CreateFolder(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    val doCreate: (StateID, String) -> Unit = { parentID, name ->
        moreMenuVM.createFolder(parentID, name)
        // TODO implement a user feedback via flows
//                if (Str.notEmpty(errMsg)) {
//                    showMessage(ctx, errMsg!!)
//                } else {
//                    browseRemoteVM.watch(parentID) // This force resets the backoff ticker
//                    showMessage(ctx, "Folder created at ${parentID.file}.")
//                }
    }

    AskForFolderName(
        parStateID = stateID,
        createFolderAt = { parentId, name ->
            doCreate(parentId, name)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

//@Composable
//fun DownloadToDevice(
//    moreMenuVM: MoreMenuVM,
//    stateID: StateID,
//    dismiss: (Boolean) -> Unit,
//) {
//
//    val launched: MutableState<Boolean> = rememberSaveable {
//        mutableStateOf(false)
//    }
//    Log.e(logTag, "----- Here for $stateID. Already launched: ${launched.value}")
//
//    val destinationPicker = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.CreateDocument(),
//        onResult = { uri ->
//            Log.e(logTag, "Got a destination for $stateID")
//            dismiss(true)
//        }
//    )
//    // TODO is this correct? we provide a dummy content event if we do not want to show anything
//    Spacer(modifier = Modifier.height(1.dp))
//
//    if (!launched.value ) {
//        destinationPicker.launch(stateID.fileName)
//    }
//}

@Composable
fun TreeNodeRename(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    val doRename: (StateID, String) -> Unit = { srcID, name ->
        moreMenuVM.renameNode(srcID, name)
    }

    AskForNewName(
        srcID = stateID,
        rename = { srcID, name ->
            doRename(srcID, name)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ConfirmDeletion(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.Delete,
        title = stringResource(id = R.string.confirm_move_to_recycle_title),
        desc = stringResource(id = R.string.confirm_move_to_recycle_desc, stateID.fileName),
        confirm = {
            moreMenuVM.deleteNode(stateID)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}
