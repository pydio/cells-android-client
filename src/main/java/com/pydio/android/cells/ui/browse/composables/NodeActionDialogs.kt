package com.pydio.android.cells.ui.browse.composables

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.composables.animations.SmoothLinearProgressIndicator
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.core.composables.dialogs.AskForFolderName
import com.pydio.android.cells.ui.core.composables.dialogs.AskForNewName
import com.pydio.android.cells.ui.models.DownloadVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay

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
fun Download(
    downloadVM: DownloadVM,
    stateID: StateID,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
    val rTransfer = downloadVM.transfer.collectAsState(null)
    val rTreeNode = downloadVM.treeNode.collectAsState()

    LaunchedEffect(key1 = rTransfer.value?.status) {
        val status = rTransfer.value?.status
        if (JobStatus.DONE.id == status) {
            try {
                downloadVM.viewFile(context)
            } catch (se: SDKException) {
                if (se.code == ErrorCodes.no_local_file) {
                    // FIXME give more time to register new file
                    delay(2000L)
                    try {
                        downloadVM.viewFile(context)
                    } catch (e: Exception) {
                        Log.e(logTag, "File has been downloaded but is not found")
                        e.printStackTrace()
                    }
                }
            }
            dismiss()
        }
    }

    val progress = rTransfer.value?.let {
        if (it.byteSize > 0) {
            it.progress.toFloat().div(it.byteSize.toFloat())
        } else {
            0f
        }
    } ?: 0f

    AlertDialog(
        title = { Text(stringResource(R.string.running_download_title)) },
        text = {
            Column {
                Text(text = rTreeNode.value?.name ?: "")
                SmoothLinearProgressIndicator(
                    indicatorProgress = progress,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.margin))
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = dismiss) { Text(stringResource(R.string.button_run_in_background)) }
        },
        confirmButton = {
            Button(
                onClick = {
                    downloadVM.cancelDownload()
                    dismiss()
                }
            ) { Text(stringResource(R.string.button_cancel)) }
        },
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit
        )
    )
    LaunchedEffect(key1 = stateID) {
        downloadVM.launchDownload()
    }
}

@Composable
fun ChooseDestination(
    nodeActionsVM: NodeActionsVM,
    stateID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    Log.d(logTag, "Composing ChooseDestination for $stateID")
    val alreadyLaunched = rememberSaveable { mutableStateOf(false) }
    val destinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            Log.e(logTag, "Got a destination for $stateID")
            if (stateID != StateID.NONE) {
                uri?.let {
                    nodeActionsVM.download(stateID, uri)
                }
            }
            dismiss(true)
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.02f),
        content = {}
    )
    if (!alreadyLaunched.value) {
        LaunchedEffect(key1 = stateID) {
            Log.e(logTag, "Launching 'pick destination' for $stateID")
            delay(100)
            destinationPicker.launch(stateID.fileName)
            alreadyLaunched.value = true
        }
    }
}

@Composable
fun ImportFile(
    nodeActionsVM: NodeActionsVM,
    targetParentID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    val alreadyLaunched = rememberSaveable { mutableStateOf(false) }
    val fileImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            nodeActionsVM.importFiles(targetParentID, uris)
            dismiss(true)
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.02f),
        content = {}
    )
    if (!alreadyLaunched.value) {
        LaunchedEffect(key1 = targetParentID) {
            Log.e(logTag, "Launching 'import file' to $targetParentID")
            delay(100)
            fileImporter.launch("*/*")
            alreadyLaunched.value = true
        }
    }
}

@Composable
fun TakePicture(
    nodeActionsVM: NodeActionsVM,
    targetParentID: StateID,
    dismiss: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val alreadyLaunched = rememberSaveable { mutableStateOf(false) }
    val photoTaker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { taken ->
            if (taken) {
                nodeActionsVM.uploadPhoto()
            } else {
                nodeActionsVM.cancelPhoto()
            }
            dismiss(taken)
        }
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.02f),
        content = {}
    )
    if (!alreadyLaunched.value) {
        LaunchedEffect(key1 = targetParentID) {
            Log.e(logTag, "Launching 'TakePicture' with parent $targetParentID")
            delay(100)
            nodeActionsVM.preparePhoto(context, targetParentID)?.also {
                photoTaker.launch(it)
            }
            alreadyLaunched.value = true
        }
    }
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
        nodeActionsVM.getShareLink(stateID)?.let { linkStr ->
            val bitMatrix = writer.encode(
                linkStr,
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
