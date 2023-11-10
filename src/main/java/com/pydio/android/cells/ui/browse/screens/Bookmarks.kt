package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.R
import com.pydio.android.cells.ServerConnection
import com.pydio.android.cells.services.ConnectionState
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.composables.BookmarkListItem
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.NodesMoreMenuData
import com.pydio.android.cells.ui.browse.menus.SetMoreMenuState
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.composables.MultiSelectTopBar
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.lists.MultipleGridItem
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val LOG_TAG = "Bookmarks.kt"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bookmarks(
    isExpandedScreen: Boolean,
    accountID: StateID,
    openDrawer: () -> Unit,
    bookmarksVM: BookmarksVM,
    browseHelper: BrowseHelper,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val connectionState by bookmarksVM.connectionState.collectAsState(
        ConnectionState(
            LoadingState.IDLE,
            ServerConnection.OK
        )
    )
    val listLayout by bookmarksVM.layout.collectAsState(ListLayout.LIST)

    val multiSelectData: MutableState<Set<StateID>> = rememberSaveable {
        mutableStateOf(setOf())
    }

    val bookmarks = bookmarksVM.bookmarks.collectAsState(listOf())
    val forceRefresh: () -> Unit = {
        bookmarksVM.forceRefresh(accountID)
    }

    val itemTapped: (StateID, Boolean) -> Unit = { stateID, longPress ->
        scope.launch {
            if (multiSelectData.value.isEmpty()) {
                if (longPress) { // Toggle to multi select mode and add the element
                    multiSelectData.value = setOf(stateID)
                } else { // short click
                    browseHelper.open(context, stateID, browseHelper.bookmarks)
                }
            } else { // Already in multiselect node, toggle current node
                val old = multiSelectData.value
                if (old.contains(stateID)) {
                    multiSelectData.value = old.minus(stateID)
                } else {
                    multiSelectData.value = old.plus(stateID)
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, Set<StateID>>> = remember {
        mutableStateOf(NodeMoreMenuType.NONE to setOf())
    }

    val openMoreMenu: (NodeMoreMenuType, Set<StateID>) -> Unit = { type, stateIDs ->
        scope.launch {
            nodeMoreMenuData.value = type to stateIDs
            sheetState.expand()
        }
    }

    val moreMenuDone: () -> Unit = {
        scope.launch {
            sheetState.hide()
            nodeMoreMenuData.value = NodeMoreMenuType.BOOKMARK to setOf()
            multiSelectData.value = setOf()
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri ->
            val currSet = nodeMoreMenuData.value.second
            if (currSet.size == 1) {
                uri?.let {
                    bookmarksVM.download(currSet.first(), uri)
                }
            }
            moreMenuDone()
        }
    )

    val launchMulti: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
        when (action) {
            is NodeAction.ToggleBookmark -> {
                for (stateID in stateIDs) {
                    bookmarksVM.removeBookmark(stateID)
                }
                moreMenuDone()
            }

            is NodeAction.UnSelectAll -> {
                moreMenuDone()
            }

            else -> {
                Log.e(LOG_TAG, "unexpected action: $action")
            }
        }
    }

    val launchMono: (NodeAction, StateID) -> Unit = { action, stateID ->
        when (action) {
            is NodeAction.OpenInApp -> {
                scope.launch {
                    bookmarksVM.getNode(stateID)?.let {
                        if (it.isFolder()) {
                            browseHelper.open(context, stateID, browseHelper.bookmarks)
                        } else {
                            browseHelper.open(context, stateID.parent(), browseHelper.bookmarks)
                        }
                    }
                }
                moreMenuDone()
            }

            is NodeAction.DownloadToDevice -> {
                destinationPicker.launch(stateID.fileName)
                // Done is called by the destination picker callback
            }

            is NodeAction.ToggleBookmark -> {
                bookmarksVM.removeBookmark(stateID)
                moreMenuDone()
            }

            is NodeAction.AsGrid -> {
                bookmarksVM.setListLayout(ListLayout.GRID)
            }

            is NodeAction.AsList -> {
                bookmarksVM.setListLayout(ListLayout.LIST)
            }

            is NodeAction.SortBy -> { // The real set has already been done by the bottom sheet via its preferencesVM
                moreMenuDone()
            }

            else -> {
                Log.e(LOG_TAG, "Unknown action $action for $stateID")
                moreMenuDone()
            }
        }
    }

    val launch: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
        if (stateIDs.size == 1) {
            launchMono(action, stateIDs.first())
        } else {
            launchMulti(action, stateIDs)
        }
    }

    BookmarkScaffold(
        isExpandedScreen = isExpandedScreen,
        connectionState = connectionState,
        listLayout = listLayout,
        title = stringResource(id = R.string.action_open_bookmarks),
        bookmarks = bookmarks.value,
        openDrawer = openDrawer,
        forceRefresh = forceRefresh,
        onTap = itemTapped,
        launch = launch,
        moreMenuState = SetMoreMenuState(
            sheetState = sheetState,
            type = nodeMoreMenuData.value.first,
            stateIDs = nodeMoreMenuData.value.second,
            openMoreMenu = openMoreMenu,
            cancelSelection = { multiSelectData.value = setOf() }
        ),
        selectedItems = multiSelectData.value
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkScaffold(
    isExpandedScreen: Boolean,
    connectionState: ConnectionState,
    listLayout: ListLayout,
    title: String,
    bookmarks: List<MultipleItem>,
    openDrawer: () -> Unit,
    forceRefresh: () -> Unit,
    onTap: (StateID, Boolean) -> Unit,
    launch: (NodeAction, Set<StateID>) -> Unit,
    moreMenuState: SetMoreMenuState,
    selectedItems: Set<StateID>
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
                    launch(NodeAction.AsList, setOf(StateID.NONE))
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsList, label) },
            )
        } else {
            val label = stringResource(R.string.button_switch_to_grid_layout)
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    launch(NodeAction.AsGrid, setOf(StateID.NONE))
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
                    setOf(StateID.NONE)
                )
                showMenu(false)
            },
            leadingIcon = { Icon(CellsIcons.SortBy, label) },
        )
    }

    Scaffold(
        topBar = {
            if (selectedItems.isNotEmpty()) {
                MultiSelectTopBar(
                    selected = selectedItems,
                    cancel = moreMenuState.cancelSelection,
                    isMoreMenuShown = moreMenuState.sheetState.isVisible,
                    showMenu = {
                        if (it) {
                            moreMenuState.openMoreMenu(
                                NodeMoreMenuType.BOOKMARK,
                                selectedItems
                            )
                        }
                    },
                )
            } else {
                TopBarWithMoreMenu(
                    title = title,
                    isExpandedScreen = isExpandedScreen,
                    openDrawer = openDrawer,
                    isActionMenuShown = isShown,
                    showMenu = showMenu,
                    content = actionMenuContent
                )
            }
        },
    ) { padding ->
        CellsModalBottomSheetLayout(
            isExpandedScreen = isExpandedScreen,
            sheetContent = {
                if (moreMenuState.stateIDs.size == 1) {
                    NodeMoreMenuData(
                        connectionState = connectionState,
                        type = moreMenuState.type,
                        subjectID = moreMenuState.stateIDs.first(),
                        launch = { a, s -> launch(a, setOf(s)) },
                    )
                } else if (moreMenuState.stateIDs.size > 1) {
                    NodesMoreMenuData(
                        connectionState = connectionState,
                        type = NodeMoreMenuType.BOOKMARK,
                        subjectIDs = moreMenuState.stateIDs,
                        launch = launch,
                    )
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            },
            sheetState = moreMenuState.sheetState,
        ) {
            BookmarkList(
                loadingState = connectionState,
                listLayout = listLayout,
                isSelectionMode = selectedItems.isNotEmpty(),
                bookmarks = bookmarks,
                selectedItems = selectedItems,
                forceRefresh = forceRefresh,
                openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.BOOKMARK, setOf(it)) },
                onTap = onTap,
                padding = padding,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BookmarkList(
    loadingState: ConnectionState,
    listLayout: ListLayout,
    isSelectionMode: Boolean,
    bookmarks: List<MultipleItem>,
    selectedItems: Set<StateID>,
    forceRefresh: () -> Unit,
    openMoreMenu: (StateID) -> Unit,
    onTap: (StateID, Boolean) -> Unit,
    padding: PaddingValues,
) {

    val state = rememberPullRefreshState(
        loadingState.loading == LoadingState.PROCESSING,
        onRefresh = {
            Log.i(LOG_TAG, "Force refresh launched")
            forceRefresh()
        },
    )
    WithLoadingListBackground(
        connectionState = loadingState,
        isEmpty = bookmarks.isEmpty(),
        listContext = ListContext.BOOKMARKS,
        emptyRefreshableDesc = stringResource(R.string.no_bookmark_for_account),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pullRefresh(state)
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
                        columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_large_col_min_width)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                        contentPadding = listPadding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = bookmarks,
                            key = { it.uuid }
                        ) { node ->
                            MultipleGridItem(
                                item = node,
                                more = { openMoreMenu(node.defaultStateID()) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItems.contains(node.defaultStateID()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onTap(node.defaultStateID(), false) },
                                        onLongClick = { onTap(node.defaultStateID(), true) },
                                    )
                                    .animateItemPlacement(),
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = padding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bookmarks, key = { it.uuid }) { node ->
                            BookmarkListItem(
                                item = node,
                                more = { openMoreMenu(node.defaultStateID()) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItems.contains(node.defaultStateID()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onTap(node.defaultStateID(), false) },
                                        onLongClick = { onTap(node.defaultStateID(), true) },
                                    )
                                    .animateItemPlacement()
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                loadingState.loading == LoadingState.PROCESSING,
                state,
                Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
