package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTopAppBarState
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
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.browse.models.TransfersVM
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.share.TransferBottomSheet
import com.pydio.android.cells.ui.share.TransferListItem
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val logTag = "TransferScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Transfers(
    openDrawer: () -> Unit,
    open: (StateID) -> Unit,
    transfersVM: TransfersVM,
) {

    val currTransfers = transfersVM.transfers.observeAsState()

    WithBottomSheet(
        transfersVM,
        transfers = currTransfers.value ?: listOf(),
        openDrawer = openDrawer,
        pauseOne = transfersVM::pauseOne,
        resumeOne = transfersVM::resumeOne,
        removeOne = transfersVM::removeOne,
        cancelOne = transfersVM::cancelOne,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun WithBottomSheet(
    uploadsVM: TransfersVM,
    transfers: List<RTransfer>,
    openDrawer: () -> Unit,
    pauseOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
) {

    val scope = rememberCoroutineScope()

    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)

    val transferState: MutableState<RTransfer?> = remember {
        mutableStateOf(null)
    }

    val openMoreMenu: (Long) -> Unit = { transferId ->
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
            var hide = true
            when (action) {
                AppNames.ACTION_MORE -> {
                    openMoreMenu(transferId)
                    hide = false
                }
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
            if (hide) {
                transferState.value = null
                state.hide()
            }
        }
    }

    ModalBottomSheetLayout(
        sheetContent = { TransferBottomSheet(transferState.value, doAction) },
        modifier = Modifier,
        sheetState = state,
    ) {
        WithScaffold(
            transfers = transfers,
            doAction = doAction,
            openDrawer = openDrawer,
            modifier = Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    transfers: List<RTransfer>,
    doAction: (String, Long) -> Unit,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topAppBarState = rememberTopAppBarState()
    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.transfer_list_title),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        TransferList(
            transfers,
            doAction,
            innerPadding,
        )
    }
}

@Composable
private fun TransferList(
    transfers: List<RTransfer>,
    doAction: (String, Long) -> Unit,
    innerPadding: PaddingValues
) {
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(innerPadding)
    ) {
        items(transfers) { transfer ->
            TransferListItem(
                transfer,
                pause = { doAction(AppNames.ACTION_CANCEL, transfer.transferId) },
                resume = { doAction(AppNames.ACTION_RESTART, transfer.transferId) },
                remove = { doAction(AppNames.ACTION_DELETE_RECORD, transfer.transferId) },
                more = { doAction(AppNames.ACTION_MORE, transfer.transferId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.card_padding))
                    .wrapContentWidth(Alignment.Start)
            )
        }
    }
}
