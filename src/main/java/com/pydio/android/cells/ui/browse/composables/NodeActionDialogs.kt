package com.pydio.android.cells.ui.browse.composables

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.MoreMenuVM
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.core.composables.dialogs.AskForFolderName
import com.pydio.android.cells.ui.core.composables.dialogs.AskForNewName
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

private const val logTag = "NodeActionDialogs.kt"

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

@Composable
fun ConfirmPermanentDeletion(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.Delete,
        title = stringResource(id = R.string.confirm_permanent_deletion_title),
        desc = stringResource(id = R.string.confirm_permanent_deletion_desc, stateID.fileName),
        confirm = {
            moreMenuVM.deleteNode(stateID) // TODO this should be enough
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ConfirmEmptyRecycle(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.EmptyRecycle,
        title = stringResource(id = R.string.confirm_permanent_deletion_title),
        desc = stringResource(id = R.string.confirm_empty_recycle_message, stateID.fileName),
        confirm = {
            moreMenuVM.emptyRecycle(stateID) // TODO this should be enough
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ShowQRCode(
    moreMenuVM: MoreMenuVM,
    stateID: StateID,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
//    val gotBitmap = rememberSaveable {
//        mutableStateOf(false)
//    }

    val bitmap = remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    val writer = QRCodeWriter()
    LaunchedEffect(stateID) {
        Log.e(logTag, "in launched effect")
        moreMenuVM.getShareLink(stateID)?.let {
            val bitMatrix = writer.encode(
                it,
                BarcodeFormat.QR_CODE,
                context.resources.getInteger(R.integer.qrcode_width),
                context.resources.getInteger(R.integer.qrcode_width)
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    tmpBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            Log.e(logTag, "got a bitmap")
            bitmap.value = tmpBitmap.asImageBitmap()
        }
    }

    bitmap.value?.let {
        AlertDialog(
            title = { Text(stringResource(R.string.display_as_qrcode_dialog_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.display_as_qrcode_dialog_desc, stateID.path))
                    Spacer(
                        modifier = Modifier.size(dimensionResource(id = R.dimen.margin_medium))
                    )
                    Image(
                        bitmap = it,
                        contentDescription = /* TODO */ "",
                        modifier = Modifier.size(200.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.button_ok)) }
            },
            onDismissRequest = dismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                securePolicy = SecureFlagPolicy.Inherit
            )
        )
    }

}
