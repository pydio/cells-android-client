package com.pydio.android.cells.ui.box.system

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.models.TransferVM

private const val logTag = "UploadProgressList"

@Composable
fun UploadProgressList(
    transferVM: TransferVM,
    runInBackground: () -> Unit,
) {
    val currTransfers = transferVM.currRecords.observeAsState()

    UploadProgressList(
        uploads = currTransfers.value ?: listOf(),
        runInBackground = runInBackground,
        pauseOne = transferVM::pauseOne,
        resumeOne = transferVM::resumeOne,
        removeOne = transferVM::removeOne,
        cancelOne = transferVM::cancelOne,
        cancelAll = transferVM::cancelAll,
    )
}

@Composable
fun UploadProgressList(
    uploads: List<RTransfer>,
    runInBackground: () -> Unit,
    pauseOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
    cancelAll: () -> Unit,
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
                    pause = {pauseOne(upload.transferId)},
                    resume = {resumeOne(upload.transferId)},
                    remove = { removeOne(upload.transferId) },
                    more = { /* TODO */  },
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
