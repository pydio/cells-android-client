package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListType
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.browse.menus.FilterTransfersByMenu
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.browse.menus.TransferMoreMenu
import com.pydio.android.cells.ui.browse.menus.TransferMoreMenuState
import com.pydio.android.cells.ui.browse.menus.TransferMoreMenuType
import com.pydio.android.cells.ui.browse.models.TransfersVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.share.composables.TransferListItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val logTag = "TransferScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Transfers(
    accountID: StateID,
    transfersVM: TransfersVM,
    openDrawer: () -> Unit,
    open: (StateID) -> Unit,
) {

    val loadingState by transfersVM.loadingState.observeAsState()
    val currTransfers = transfersVM.transfers.observeAsState()

    WithState(
        loadingState = loadingState ?: LoadingState.STARTING,
        forceRefresh = transfersVM::forceRefresh,
        accountID = accountID,
        transfers = currTransfers.value ?: listOf(),
        transfersVM = transfersVM,
        openDrawer = openDrawer,
        open = open,
        pauseOne = transfersVM::pauseOne,
        resumeOne = transfersVM::resumeOne,
        removeOne = transfersVM::removeOne,
        cancelOne = transfersVM::cancelOne,
    )
}

@Composable
@ExperimentalMaterial3Api
private fun WithState(
    loadingState: LoadingState,
    forceRefresh: () -> Unit,
    accountID: StateID,
    transfers: List<RTransfer>,
    transfersVM: TransfersVM,
    openDrawer: () -> Unit,
    open: (StateID) -> Unit,
    pauseOne: (Long) -> Unit,
    resumeOne: (Long) -> Unit,
    removeOne: (Long) -> Unit,
    cancelOne: (Long) -> Unit,
) {

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val transferMoreMenuData: MutableState<Pair<TransferMoreMenuType, Long>> = remember {
        mutableStateOf(Pair(TransferMoreMenuType.NONE, -1L))
    }

    val openMoreMenu: (TransferMoreMenuType, Long) -> Unit = { type, transferID ->
        scope.launch {
            // Log.i(logTag, "About to open $type more menu for transfer #$transferID")
            transferMoreMenuData.value = Pair(type, transferID)
            sheetState.expand()
        }
    }

    val closeMoreMenu: () -> Unit = {
        scope.launch {
            sheetState.hide()
            transferMoreMenuData.value = Pair(TransferMoreMenuType.NONE, -1L)
        }
    }

    val doAction: (String, Long) -> Unit = { action, transferID ->

        when (action) {
            AppNames.ACTION_MORE -> {
                openMoreMenu(TransferMoreMenuType.MORE, transferID)
            }
            AppNames.ACTION_CANCEL -> {
                pauseOne(transferID)
            }
            AppNames.ACTION_RESTART -> {
                resumeOne(transferID)
            }
            AppNames.ACTION_DELETE_RECORD -> {
                removeOne(transferID)
            }
            AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES -> {
                // It is always a file for the time being => we open the parent
                scope.launch {
                    transfersVM.get(transferID)?.let { rTransfer ->
                        rTransfer.getStateId()?.let {
                            // We still have to explicitly call this otherwise the scrim is still here
                            //  when we pass here and then come back using android nav back button
                            //  (maybe just on an overloaded AVD)
                            sheetState.hide()
                            open(it.parent())
                        }
                    }
                }
            }
        }
    }

    WithBottomSheet(
        loadingState = loadingState,
        forceRefresh = forceRefresh,
        accountID = accountID,
        transfers = transfers,
        moreMenuState = TransferMoreMenuState(
            transferMoreMenuData.value.first,
            sheetState,
            transferMoreMenuData.value.second,
            openMoreMenu,
            closeMoreMenu = closeMoreMenu
        ),
        doAction = doAction,
        clearTerminated = transfersVM::clearTerminated,
        openDrawer = openDrawer,
        modifier = Modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithBottomSheet(
    loadingState: LoadingState,
    forceRefresh: () -> Unit,
    accountID: StateID,
    transfers: List<RTransfer>,
    moreMenuState: TransferMoreMenuState,
    doAction: (String, Long) -> Unit,
    clearTerminated: () -> Unit,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val tint = MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface

    ModalBottomSheetLayout(
        sheetContent = {
            when (moreMenuState.type) {
                TransferMoreMenuType.SORT_BY ->
                    SortByMenu(
                        type = ListType.TRANSFER,
                        done = moreMenuState.closeMoreMenu,
                        tint = tint,
                        bgColor = bgColor,
                    )
                TransferMoreMenuType.FILTER_BY ->
                    FilterTransfersByMenu(
                        done = moreMenuState.closeMoreMenu,
                        tint = tint,
                        bgColor = bgColor,
                    )
                TransferMoreMenuType.MORE -> {
                    TransferMoreMenu(
                        accountID = accountID,
                        transferID = moreMenuState.transferID,
                        onClick = { action, transferID ->
                            doAction(action, transferID)
                            moreMenuState.closeMoreMenu()
                        }
                    )
                }
                else -> {
                    // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
                    // when no item is defined (default case at starting point)
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        },
        modifier = Modifier,
        sheetState = moreMenuState.sheetState,
    ) {
        WithScaffold(
            loadingState = loadingState,
            forceRefresh = forceRefresh,
            transfers = transfers,
            doAction = doAction,
            openDrawer = openDrawer,
            clearTerminated = clearTerminated,
            moreMenuState = moreMenuState,
            modifier = Modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    loadingState: LoadingState,
    forceRefresh: () -> Unit, doAction: (String, Long) -> Unit,
    transfers: List<RTransfer>,
    moreMenuState: TransferMoreMenuState,
    openDrawer: () -> Unit,
    clearTerminated: () -> Unit,
    modifier: Modifier = Modifier
) {

    var isShown by remember { mutableStateOf(false) }
    val showMenu: (Boolean) -> Unit = {
        if (it != isShown) {
            isShown = it
        }
    }

    val actionMenuContent: @Composable ColumnScope.() -> Unit = {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.button_open_sort_by)) },
            onClick = {
                moreMenuState.openMoreMenu(
                    TransferMoreMenuType.SORT_BY,
                    0L
                )
                showMenu(false)
            },
            leadingIcon = { Icon(CellsIcons.SortBy, stringResource(R.string.button_open_sort_by)) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.button_open_filter_by)) },
            onClick = {
                moreMenuState.openMoreMenu(
                    TransferMoreMenuType.FILTER_BY,
                    0L
                )
                showMenu(false)
            },
            leadingIcon = {
                Icon(
                    CellsIcons.FilterBy,
                    stringResource(R.string.button_open_filter_by)
                )
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clear_terminated)) },
            onClick = {
                clearTerminated()
                showMenu(false)
            },
            leadingIcon = {
                Icon(
                    CellsIcons.DeleteForever,
                    stringResource(R.string.clear_terminated)
                )
            },
        )
    }

    Scaffold(
        topBar = {
            TopBarWithMoreMenu(
                title = stringResource(R.string.transfer_list_title),
                openDrawer = openDrawer,
                isActionMenuShown = isShown,
                showMenu = showMenu,
                content = actionMenuContent
            )
        },
        modifier = modifier
    ) { innerPadding ->
        TransferList(
            loadingState,
            forceRefresh,
            transfers,
            doAction,
            innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun TransferList(
    loadingState: LoadingState,
    forceRefresh: () -> Unit,
    transfers: List<RTransfer>,
    doAction: (String, Long) -> Unit,
    innerPadding: PaddingValues
) {

    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh()
    })

    WithLoadingListBackground(
        loadingState = loadingState,
        isEmpty = transfers.isEmpty(),
        emptyRefreshableDesc = stringResource(R.string.no_transfer_for_account),
        canRefresh = true,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pullRefresh(state)
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

            PullRefreshIndicator(
                loadingState == LoadingState.PROCESSING,
                state,
                Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
