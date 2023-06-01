package com.pydio.android.cells.ui.share.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.share.TransferBottomSheet
import com.pydio.android.cells.ui.share.composables.TransferListItem
import com.pydio.android.cells.ui.share.models.MonitorUploadsVM
import kotlinx.coroutines.launch

private const val logTag = "UploadProgressList"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadProgressList(
    uploadsVM: MonitorUploadsVM,
    runInBackground: () -> Unit,
    done: () -> Unit,
    cancel: () -> Unit,
    openParentLocation: () -> Unit,
) {

    val currJob = uploadsVM.parentJob.collectAsState(null)
    val currTransfers = uploadsVM.currRecords.collectAsState(listOf())

    UploadProgressList(
        uploadsVM = uploadsVM,
        openParentLocation = openParentLocation,
        currJob = currJob.value,
        uploads = currTransfers.value,
        runInBackground = runInBackground,
        done = done,
        cancel = cancel,
        pauseOne = uploadsVM::pauseOne,
        cancelOne = uploadsVM::cancelOne,
        resumeOne = uploadsVM::resumeOne,
        removeOne = uploadsVM::removeOne,
//        cancelAll = uploadsVM::cancelAll,
    )
}

@Composable
@ExperimentalMaterial3Api
fun UploadProgressList(
    uploadsVM: MonitorUploadsVM,
    openParentLocation: () -> Unit,
    currJob: RJob?,
    uploads: List<RTransfer>,
    runInBackground: () -> Unit,
    done: () -> Unit,
    cancel: () -> Unit,
    pauseOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
//    cancelAll: () -> Unit,
) {

    val scope = rememberCoroutineScope()

    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val transferState: MutableState<RTransfer?> = remember {
        mutableStateOf(null)
    }

    val jobStatus = remember(key1 = currJob, key2 = uploads) {
        derivedStateOf {
            uploadsVM.getStatusFromListAndJob(currJob, uploads)
        }
    }

    val openMenu: (Long) -> Unit = { transferId ->
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
                    // TODO we still have am issue: the scrim is not removed when we come back from the target parent location
                    openParentLocation()
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
        WithScaffold(
            isRemoteServerLegacy = uploadsVM.isRemoteLegacy,
            jobStatus = jobStatus.value,
            uploads = uploads,
            runInBackground = runInBackground,
            done = done,
            cancel = cancel,
            pauseOne = pauseOne,
            cancelOne = cancelOne,
            resumeOne = resumeOne,
            removeOne = removeOne,
            openMenu = openMenu,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    isRemoteServerLegacy: Boolean,
    jobStatus: JobStatus,
    uploads: List<RTransfer>,
    runInBackground: () -> Unit,
    done: () -> Unit,
    cancel: () -> Unit,
    openMenu: (Long) -> Unit,
    pauseOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            DefaultTopBar(
                title = if (jobStatus == JobStatus.DONE)
                    stringResource(R.string.upload_screen_done_title)
                else
                    stringResource(R.string.upload_screen_pending_title),
            )
        },
    ) { innerPadding ->
        UploadList(
            isRemoteServerLegacy,
            jobStatus,
            uploads = uploads,
            runInBackground = runInBackground,
            done = done,
            pauseOne = pauseOne,
            cancelOne = cancelOne,
            resumeOne = resumeOne,
            removeOne = removeOne,
            openMenu = openMenu,
            paddingValues = innerPadding,
        )
    }
}

@Composable
fun UploadList(
    isRemoteServerLegacy: Boolean,
    jobStatus: JobStatus,
    uploads: List<RTransfer>,
    runInBackground: () -> Unit,
    done: () -> Unit,
    openMenu: (Long) -> Unit,
    pauseOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    paddingValues: PaddingValues
) {
    Column(Modifier.padding(paddingValues)) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(uploads) { upload ->
                TransferListItem(
                    isRemoteServerLegacy,
                    upload,
                    pause = { pauseOne(upload.transferId) },
                    cancel = { cancelOne(upload.transferId) },
                    resume = { resumeOne(upload.transferId) },
                    remove = { removeOne(upload.transferId) },
                    more = { openMenu(upload.transferId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.card_padding))
                        .wrapContentWidth(Alignment.Start)
                )
            }
        }

        // TODO also handle errors
        if (jobStatus == JobStatus.DONE) {
            TextButton(
                onClick = {
                    done()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = dimensionResource(R.dimen.margin))
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.button_done_and_exit)) }
        } else {
            TextButton(
                onClick = {
                    runInBackground()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = dimensionResource(R.dimen.margin))
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.button_run_in_background)) }

            // TODO also add a cancel button and implement corresponding cleaning
        }
    }
}
