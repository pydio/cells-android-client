package com.pydio.android.cells.ui.share.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.share.TransferBottomSheet
import com.pydio.android.cells.ui.share.TransferListItem
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import kotlinx.coroutines.launch

private const val logTag = "UploadProgressList"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadProgressList(
    uploadsVM: MonitorUploadsVM,
    runInBackground: () -> Unit,
) {
    val currTransfers = uploadsVM.currRecords.observeAsState()

    UploadProgressList(
        uploadsVM,
        uploads = currTransfers.value ?: listOf(),
        runInBackground = runInBackground,
        pauseOne = uploadsVM::pauseOne,
        resumeOne = uploadsVM::resumeOne,
        removeOne = uploadsVM::removeOne,
        cancelOne = uploadsVM::cancelOne,
        cancelAll = uploadsVM::cancelAll,
    )
}

@Composable
@ExperimentalMaterial3Api
fun UploadProgressList(
    uploadsVM: MonitorUploadsVM,
    uploads: List<RTransfer>,
    runInBackground: () -> Unit,
    pauseOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
    cancelAll: () -> Unit,
) {

    val scope = rememberCoroutineScope()

    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val transferState: MutableState<RTransfer?> = remember {
        mutableStateOf(null)
    }

    val openScaffold: (Long) -> Unit = { transferId ->
        scope.launch {
            val currTransfer = uploadsVM.get(transferId) ?: run {
                Log.e(logTag, "No transfer found with ID $transferId, aborting")
                return@launch
            }
            transferState.value = currTransfer
            state.expand()
        }
    }

    val doAction: (String, Long) -> Unit = { action, transferId ->
        scope.launch {
            when (action) {
                AppNames.ACTION_CANCEL -> {
                    pauseOne(transferId)
                }
                AppNames.ACTION_RESTART -> {
                    resumeOne(transferId)
                }
                AppNames.ACTION_DELETE_RECORD -> {
                    removeOne(transferId)
                }
                AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES -> {
                    // TODO
                }
            }
            transferState.value = null
            state.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetContent = { TransferBottomSheet(transferState.value, doAction) },
        modifier = Modifier,
        state
    ) {
        Column {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(uploads) { upload ->
                    TransferListItem(
                        upload,
                        pause = { pauseOne(upload.transferId) },
                        resume = { resumeOne(upload.transferId) },
                        remove = { removeOne(upload.transferId) },
                        more = { openScaffold(upload.transferId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.card_padding))
                            .wrapContentWidth(Alignment.Start)
                    )
                }
            }

            TextButton(
                onClick = {
                    runInBackground()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = dimensionResource(R.dimen.margin))
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.button_run_in_background)) }
        }
    }
}
