package com.pydio.android.cells.ui.search

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.ListType
import com.pydio.android.cells.R
import com.pydio.android.cells.services.ConnectionState
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.menus.MoreMenuState
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.composables.TopBarWithSearch
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val logTag = "Search"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
    isExpandedScreen: Boolean,
    queryContext: String,
    stateID: StateID,
    searchVM: SearchVM = koinViewModel(),
    searchHelper: SearchHelper,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    searchVM.newContext(queryContext, stateID)

    val connectionState by searchVM.connectionState.collectAsState()
    val errMessage by searchVM.errorMessage.collectAsState(null)
    val listLayout by searchVM.layout.collectAsState(ListLayout.LIST)

    val query by searchVM.userInput.collectAsState("")
    val hits = searchVM.newHits.collectAsState()

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, StateID>> = remember {
        mutableStateOf(Pair(NodeMoreMenuType.SEARCH, StateID.NONE))
    }
    val openMoreMenu: (NodeMoreMenuType, StateID) -> Unit = { type, currID ->
        scope.launch {
            nodeMoreMenuData.value = Pair(type, currID)
            sheetState.expand()
        }
    }

    val moreMenuDone: () -> Unit = {
        scope.launch {
            sheetState.hide()
            nodeMoreMenuData.value = Pair(NodeMoreMenuType.NONE, StateID.NONE)
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        // TODO we have the mime of the file to download to device
        //    but this is no trivial implementation: the contract must then be both
        //    dynamic AND remembered.
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            if (nodeMoreMenuData.value.second != StateID.NONE) {
                uri?.let {
                    searchVM.download(nodeMoreMenuData.value.second, uri)
                }
            }
            moreMenuDone()
        }
    )

    val launch: (NodeAction, StateID) -> Unit = { action, currID ->

        // Todo also unify with browse helper
        Log.e(logTag, "Launching action $action for $currID")

        when (action) {
            is NodeAction.OpenInApp -> {
                moreMenuDone()
                scope.launch {
                    searchHelper.open(context, currID)
                }
            }

            is NodeAction.OpenParentLocation -> {
                moreMenuDone()
                scope.launch {
                    searchHelper.openParentLocation(currID)
                }
            }

            is NodeAction.DownloadToDevice -> {
                destinationPicker.launch(currID.fileName)
                // Done is called by the destination picker callback
            }

            is NodeAction.AsGrid -> {
                searchVM.setListLayout(ListLayout.GRID)
            }

            is NodeAction.AsList -> {
                searchVM.setListLayout(ListLayout.LIST)
            }

            is NodeAction.SortBy -> { // The real set has already been done by the bottom sheet via its preferencesVM
                moreMenuDone()
            }

            else -> {
                Log.e(logTag, "Unknown action $action for $currID")
                moreMenuDone()
            }
        }
    }

    WithScaffold(
        isExpandedScreen = isExpandedScreen,
        connectionState = connectionState,
        query = query,
        errMsg = errMessage,
        updateQuery = searchVM::setQuery,
        listLayout = listLayout,
        hits = hits.value,
        open = { currID ->
            scope.launch {
                searchHelper.open(context, currID)
            }
        },
        launch = launch,
        cancel = searchHelper::cancel,
        moreMenuState = MoreMenuState(
            sheetState,
            nodeMoreMenuData.value.first,
            nodeMoreMenuData.value.second,
            openMoreMenu
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    isExpandedScreen: Boolean,
    connectionState: ConnectionState,
    query: String,
    errMsg: ErrorMessage?,
    updateQuery: (String) -> Unit,
    listLayout: ListLayout,
    hits: List<MultipleItem>,
    open: (StateID) -> Unit,
    launch: (NodeAction, StateID) -> Unit,
    cancel: () -> Unit,
    moreMenuState: MoreMenuState,
) {
    val focusManager = LocalFocusManager.current

    var isShown by remember { mutableStateOf(false) }
    val showMenu: (Boolean) -> Unit = {
        if (it != isShown) {
            isShown = it
        }
    }

    val actionMenuContent: @Composable ColumnScope.() -> Unit = {
        if (listLayout == ListLayout.GRID) {
            val label = stringResource(R.string.button_switch_to_list_layout)
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    launch(
                        NodeAction.AsList,
                        StateID.NONE
                    )
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsList, label) },
            )
        } else {
            val label = stringResource(R.string.button_switch_to_grid_layout)
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    launch(
                        NodeAction.AsGrid,
                        StateID.NONE
                    )
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsGrid, label) },
            )
        }

        val label = stringResource(R.string.button_open_sort_by)
        DropdownMenuItem(
            text = { Text(label) },
            onClick = {
                moreMenuState.openMoreMenu(
                    NodeMoreMenuType.SORT_BY,
                    StateID.NONE
                )
                showMenu(false)
            },
            leadingIcon = { Icon(CellsIcons.SortBy, label) },
        )
    }

    Scaffold(
        topBar = {
            TopBarWithSearch(
                queryStr = query,
                errorMessage = errMsg,
                updateQuery = updateQuery,
                cancel = cancel,
                isActionMenuShown = isShown,
                showMenu = showMenu,
                content = actionMenuContent
            )
        },
    ) { padding ->

        CellsModalBottomSheetLayout(
            isExpandedScreen = isExpandedScreen,
            sheetContent = {
                if (moreMenuState.type == NodeMoreMenuType.SORT_BY) {
                    SortByMenu(
                        type = ListType.DEFAULT,
                        done = { launch(NodeAction.SortBy, StateID.NONE) },
                    )
                } else {
                    NodeMoreMenuData(
                        connectionState = connectionState,
                        type = NodeMoreMenuType.SEARCH,
                        subjectID = moreMenuState.stateID,
                        launch = launch,
                    )
                }
            },
            sheetState = moreMenuState.sheetState,
        ) {
            HitsList(
                connectionState = connectionState,
                listLayout = listLayout,
                query = query,
                hits = hits,
                openMoreMenu = {
                    focusManager.clearFocus()
                    moreMenuState.openMoreMenu(NodeMoreMenuType.SEARCH, it)
                },
                open = {
                    focusManager.clearFocus()
                    open(it)
                },
                padding = padding,
            )
        }
    }
}
