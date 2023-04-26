package com.pydio.android.cells.ui.search

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListType
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.menus.MoreMenuState
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.TopBarWithSearch
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithIcon
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithThumb
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val logTag = "Search"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
    queryContext: String,
    stateID: StateID,
    searchVM: SearchVM = koinViewModel(),
    searchHelper: SearchHelper,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    searchVM.newContext(queryContext, stateID)

    val loadingState by searchVM.loadingState.observeAsState()
    val errMessage by searchVM.errorMessage.observeAsState()
    val listLayout by searchVM.layout.collectAsState(ListLayout.LIST)

    val query by searchVM.userInput.collectAsState("")
    // TODO this seems clumsy. We have a flow of liveData double check and improve
    val hits = searchVM.hits.collectAsState(null).value?.observeAsState()

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
        contract = ActivityResultContracts.CreateDocument(),
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
        loadingState = loadingState ?: LoadingState.STARTING,
        query = query,
        errMsg = errMessage,
        updateQuery = searchVM::setQuery,
        listLayout = listLayout,
        hits = hits?.value ?: listOf(),
        open = { currID ->
            scope.launch {
                searchHelper.open(context, currID)
            }
        },
        launch = launch,
        cancel = searchHelper::cancel,
        moreMenuState = MoreMenuState(
            nodeMoreMenuData.value.first,
            sheetState,
            nodeMoreMenuData.value.second,
            openMoreMenu
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    loadingState: LoadingState,
    query: String,
    errMsg: String?,
    updateQuery: (String) -> Unit,
    listLayout: ListLayout,
    hits: List<RTreeNode>,
    open: (StateID) -> Unit,
    launch: (NodeAction, StateID) -> Unit,
    cancel: () -> Unit,
    moreMenuState: MoreMenuState,
) {

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
            sheetContent = {
                if (moreMenuState.type == NodeMoreMenuType.SORT_BY) {
                    SortByMenu(
                        type = ListType.DEFAULT,
                        done = { launch(NodeAction.SortBy, StateID.NONE) },
                    )
                } else {
                    NodeMoreMenuData(
                        type = NodeMoreMenuType.SEARCH,
                        toOpenStateID = moreMenuState.stateID,
                        launch = {
                            Log.e(logTag, "Calling $it for ${moreMenuState.stateID}")
                            launch(it, moreMenuState.stateID)
                        },
                    )
                }
            },
            sheetState = moreMenuState.sheetState,
        ) {
            HitsList(
                loadingState = loadingState,
                listLayout = listLayout,
                query = query,
                hits = hits,
                openMoreMenu = {
                    // TODO hide keyboard here
//                    context.getSystemService(InputMethod.SERVICE_INTERFACE).
                    moreMenuState.openMoreMenu(NodeMoreMenuType.SEARCH, it)
                },
                open = open,
                padding = padding,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun HitsList(
    loadingState: LoadingState,
    query: String,
    listLayout: ListLayout,
    hits: List<RTreeNode>,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit,
    padding: PaddingValues,
) {

    WithLoadingListBackground(
        loadingState = loadingState,
        isEmpty = hits.isEmpty(),
        showProgressAtStartup = false,
        startingDesc = stringResource(R.string.search_hint),
        emptyRefreshableDesc = if (Str.empty(query)) {
            stringResource(R.string.search_hint, query)
        } else {
            stringResource(R.string.no_result_for_search, query)
        },
        // TODO also handle if server is unreachable
        canRefresh = true,
        modifier = Modifier.fillMaxSize()
    ) {

        when (listLayout) {
            ListLayout.GRID -> {
                val listPadding = PaddingValues(
                    top = padding.calculateTopPadding().plus(dimensionResource(R.dimen.margin)),
                    bottom = padding.calculateBottomPadding()
                        .plus(dimensionResource(R.dimen.margin)),
                    start = dimensionResource(id = R.dimen.margin_medium),
                    end = dimensionResource(id = R.dimen.margin_medium),
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_col_min_width)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = listPadding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = hits,
                        key = { it.encodedState }) { node ->
                        if (node.hasThumb()) {
                            LargeCardWithThumb(
                                stateID = node.getStateID(),
                                eTag = node.etag,
                                mime = node.mime,
                                title = getNodeTitle(name = node.name, mime = node.mime),
                                desc = getNodeDesc(
                                    node.remoteModificationTS,
                                    node.size,
                                    node.localModificationStatus
                                ),
                                openMoreMenu = {
                                    openMoreMenu(node.getStateID())
                                },
                                modifier = Modifier
                                    .clickable { open(node.getStateID()) }
                                    .animateItemPlacement(),
                                sortName = node.sortName,
                            )

                        } else {
                            LargeCardWithIcon(
                                sortName = node.sortName,
                                mime = node.mime,
                                title = getNodeTitle(name = node.name, mime = node.mime),
                                desc = getNodeDesc(
                                    node.remoteModificationTS,
                                    node.size,
                                    node.localModificationStatus
                                ),
                                openMoreMenu = { openMoreMenu(node.getStateID()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { open(node.getStateID()) }
                                    .animateItemPlacement(),
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(hits, key = { it.encodedState }) { node ->
                        NodeItem(
                            item = node,
                            title = getNodeTitle(name = node.name, mime = node.mime),
                            desc = getNodeDesc(
                                node.remoteModificationTS,
                                node.size,
                                node.localModificationStatus
                            ),
                            more = { openMoreMenu(node.getStateID()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { open(node.getStateID()) }
                                .animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }
}
