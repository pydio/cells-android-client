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
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.core.composables.dialogs.AskForFolderName
import com.pydio.android.cells.ui.core.composables.dialogs.AskForNewName
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

private const val logTag = "NodeActionDialogs.kt"

@Composable
fun CreateFolder(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {

    val doCreate: (StateID, String) -> Unit = { parentID, name ->
        nodeActionsVM.createFolder(parentID, name)
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

@Composable
fun TreeNodeRename(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    val doRename: (StateID, String) -> Unit = { srcID, name ->
        nodeActionsVM.rename(srcID, name)
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
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.Delete,
        title = stringResource(id = R.string.confirm_move_to_recycle_title),
        desc = stringResource(id = R.string.confirm_move_to_recycle_desc, stateID.fileName),
        confirm = {
            nodeActionsVM.delete(stateID)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ConfirmPermanentDeletion(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.Delete,
        title = stringResource(id = R.string.confirm_permanent_deletion_title),
        desc = stringResource(id = R.string.confirm_permanent_deletion_desc, stateID.fileName),
        confirm = {
            nodeActionsVM.delete(stateID) // TODO this should be enough
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ConfirmEmptyRecycle(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    AskForConfirmation(
        icon = CellsIcons.EmptyRecycle,
        title = stringResource(id = R.string.confirm_permanent_deletion_title),
        desc = stringResource(id = R.string.confirm_empty_recycle_message, stateID.fileName),
        confirm = {
            nodeActionsVM.emptyRecycle(stateID)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}

@Composable
fun ShowQRCode(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current

    val bitmap = remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    val writer = QRCodeWriter()
    LaunchedEffect(stateID) {
        nodeActionsVM.getShareLink(stateID)?.let {
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
