package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.R
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.composables.BookmarkListItem
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.WrapWithActions
import com.pydio.android.cells.ui.browse.menus.SetMoreMenuState
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.composables.MultiSelectTopBar
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.lists.MultipleGridItem
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.models.toErrorMessage
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
    val connectionState by bookmarksVM.connectionState.collectAsState()
    val listLayout by bookmarksVM.layout.collectAsState(ListLayout.LIST)
    val snackBarHostState = remember { SnackbarHostState() }
    val multiSelectData: MutableState<Set<StateID>> = rememberSaveable {
        mutableStateOf(setOf())
    }
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, Set<StateID>>> = remember {
        mutableStateOf(NodeMoreMenuType.NONE to setOf())
    }
    val setMoreMenuData: (NodeMoreMenuType, Set<StateID>) -> Unit = { t, ids ->
        nodeMoreMenuData.value = t to ids
    }

    // Business Objects
    val bookmarks = bookmarksVM.bookmarks.collectAsState(listOf())
    val forceRefresh: () -> Unit = {
        bookmarksVM.forceRefresh(accountID)
    }

    val errMessage by bookmarksVM.errorMessage.collectAsState()
    var oldErr: ErrorMessage? = null
    LaunchedEffect(key1 = errMessage?.defaultMessage) {
        if (oldErr == errMessage) {
            // do nothing
        } else {
            // Log.e(LOG_TAG, "Received a new message...")
            errMessage?.let {
                snackBarHostState.showSnackbar(
                    message = toErrorMessage(context, it),
                    withDismissAction = false,
                    duration = SnackbarDuration.Short
                )
                bookmarksVM.errorReceived()
            }
            oldErr = errMessage
        }
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

    val openMoreMenu: (NodeMoreMenuType, Set<StateID>) -> Unit = { type, stateIDs ->
        scope.launch {
            setMoreMenuData(type, stateIDs)
            sheetState.expand()
        }
    }

    val moreMenuDone: (Boolean) -> Unit = {
        scope.launch {
            sheetState.hide()
        }
        if (it) {
            setMoreMenuData(NodeMoreMenuType.NONE, setOf())
            multiSelectData.value = setOf()
        }
    }

//    val destinationPicker = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.CreateDocument("*/*"),
//        onResult = { uri ->
//            val currSet = nodeMoreMenuData.value.second
//            if (currSet.size == 1) {
//                uri?.let {
//                    bookmarksVM.download(currSet.first(), uri)
//                }
//            }
//            moreMenuDone(true)
//        }
//    )

//    val launchMulti: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
//        when (action) {
//            is NodeAction.ToggleBookmark -> {
//                for (stateID in stateIDs) {
//                    bookmarksVM.removeBookmark(stateID)
//                }
//                moreMenuDone(true)
//            }
//
//            is NodeAction.UnSelectAll -> {
//                moreMenuDone(true)
//            }
//
//            else -> {
//                Log.e(LOG_TAG, "unexpected action: $action")
//            }
//        }
//    }
//
//    val launchMono: (NodeAction, StateID) -> Unit = { action, stateID ->
//        when (action) {
//            is NodeAction.OpenInApp -> {
//                scope.launch {
//                    bookmarksVM.getNode(stateID)?.let {
//                        if (it.isFolder()) {
//                            browseHelper.open(context, stateID, browseHelper.bookmarks)
//                        } else {
//                            browseHelper.open(context, stateID.parent(), browseHelper.bookmarks)
//                        }
//                    }
//                }
//                moreMenuDone(true)
//            }
//
//            is NodeAction.ConfirmDownloadOnMetered -> {
//                browseHelper.navigate(actionRoute(NodeAction.ConfirmDownloadOnMetered, stateID))
//            }
//
//            is NodeAction.DownloadToDevice -> {
//                destinationPicker.launch(stateID.fileName)
//                // Done is called by the destination picker callback
//            }
//
//            is NodeAction.ToggleBookmark -> {
//                bookmarksVM.removeBookmark(stateID)
//                moreMenuDone(true)
//            }
//
//            is NodeAction.AsGrid -> {
//                bookmarksVM.setListLayout(ListLayout.GRID)
//            }
//
//            is NodeAction.AsList -> {
//                bookmarksVM.setListLayout(ListLayout.LIST)
//            }
//
//            is NodeAction.SortBy -> { // The real set has already been done by the bottom sheet via its preferencesVM
//                moreMenuDone(true)
//            }
//
//            else -> {
//                Log.e(LOG_TAG, "Unknown action $action for $stateID")
//                moreMenuDone(true)
//            }
//        }
//    }
//
//    val launch: (NodeAction, Set<StateID>) -> Unit = { action, stateIDs ->
//        if (stateIDs.size == 1) {
//            launchMono(action, stateIDs.first())
//        } else {
//            launchMulti(action, stateIDs)
//        }
//    }

    val launch: (NodeAction) -> Unit = {
        when (it) {
            is NodeAction.AsGrid -> {
                bookmarksVM.setListLayout(ListLayout.GRID)
                moreMenuDone(true)

            }

            is NodeAction.AsList -> {
                bookmarksVM.setListLayout(ListLayout.LIST)
                moreMenuDone(true)
            }

            else -> {
                Log.e(LOG_TAG, "########### Unknown action: ${it.id}")
                moreMenuDone(true)
            }
        }
    }

    WrapWithActions(
        actionDone = { done, _ -> moreMenuDone(done) },
        isExpandedScreen = isExpandedScreen,
        connectionState = connectionState,
        type = if (nodeMoreMenuData.value.first == NodeMoreMenuType.NONE)
            NodeMoreMenuType.BOOKMARK else nodeMoreMenuData.value.first,
        subjectIDs = nodeMoreMenuData.value.second,
        sheetState = sheetState,
        snackBarHostState = snackBarHostState,
    ) {

        BookmarkScaffold(
            isExpandedScreen = isExpandedScreen,
            connectionState = connectionState,
            listLayout = listLayout,
            label = stringResource(id = R.string.action_open_bookmarks),
            bookmarks = bookmarks.value,
            forceRefresh = forceRefresh,
            openDrawer = openDrawer,
            onTap = itemTapped,
            launch = launch,
            moreMenuState = SetMoreMenuState(
                sheetState = sheetState,
                type = nodeMoreMenuData.value.first,
                stateIDs = nodeMoreMenuData.value.second,
                openMoreMenu = openMoreMenu,
                cancelSelection = { multiSelectData.value = setOf() }
            ),
            selectedItems = multiSelectData.value,
            snackBarHostState = snackBarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkScaffold(
    isExpandedScreen: Boolean,
    connectionState: ConnectionState,
    listLayout: ListLayout,
    label: String,
    bookmarks: List<MultipleItem>,
    openDrawer: () -> Unit,
    openSearch: (() -> Unit)? = null,
    forceRefresh: () -> Unit,
    onTap: (StateID, Boolean) -> Unit,
    launch: (NodeAction) -> Unit,
    moreMenuState: SetMoreMenuState,
    selectedItems: Set<StateID>,
    snackBarHostState: SnackbarHostState,
) {

    var isShown by remember { mutableStateOf(false) }
    val showMenu: (Boolean) -> Unit = {
        if (it != isShown) {
            isShown = it
        }
    }

    val actionMenuContent: @Composable ColumnScope.() -> Unit = {

        if (listLayout == ListLayout.GRID) {
            val btnLabel = stringResource(R.string.button_switch_to_list_layout)
            DropdownMenuItem(
                text = { Text(btnLabel) },
                onClick = {
                    launch(NodeAction.AsList)
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsList, btnLabel) },
            )
        } else {
            val btnLabel = stringResource(R.string.button_switch_to_grid_layout)
            DropdownMenuItem(
                text = { Text(btnLabel) },
                onClick = {
                    launch(NodeAction.AsGrid)
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsGrid, btnLabel) },
            )
        }

        val btnLabel = stringResource(R.string.button_open_sort_by)
        DropdownMenuItem(
            text = { Text(btnLabel) },
            onClick = {
                moreMenuState.openMoreMenu(
                    NodeMoreMenuType.SORT_BY,
                    setOf(StateID.NONE)
                )
                showMenu(false)
            },
            leadingIcon = { Icon(CellsIcons.SortBy, btnLabel) },
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
                    isExpandedScreen = isExpandedScreen,
                    title = label,
                    openDrawer = openDrawer,
                    openSearch = openSearch,
                    isActionMenuShown = isShown,
                    showMenu = showMenu,
                    content = actionMenuContent
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding ->
//        CellsModalBottomSheetLayout(
//            isExpandedScreen = isExpandedScreen,
//            sheetContent = {
//                if (moreMenuState.stateIDs.size == 1) {
//                    NodeMoreMenuData(
//                        connectionState = connectionState,
//                        type = moreMenuState.type,
//                        subjectID = moreMenuState.stateIDs.first(),
//                        launch = { a, s -> launch(a, setOf(s)) },
//                    )
//                } else if (moreMenuState.stateIDs.size > 1) {
//                    NodesMoreMenuData(
//                        connectionState = connectionState,
//                        type = NodeMoreMenuType.BOOKMARK,
//                        subjectIDs = moreMenuState.stateIDs,
//                        launch = launch,
//                    )
//                } else {
//                    Spacer(modifier = Modifier.height(1.dp))
//                }
//            },
//            sheetState = moreMenuState.sheetState,
//        ) {
        BookmarkList(
            connectionState = connectionState,
            listLayout = listLayout,
            isSelectionMode = selectedItems.isNotEmpty(),
            bookmarks = bookmarks,
            selectedItems = selectedItems,
            forceRefresh = forceRefresh,
            openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.BOOKMARK, setOf(it)) },
            onTap = onTap,
            padding = padding,
        )
//        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BookmarkList(
    connectionState: ConnectionState,
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
        connectionState.loading == LoadingState.PROCESSING,
        onRefresh = {
            Log.i(LOG_TAG, "Force refresh launched")
            forceRefresh()
        },
    )
    WithLoadingListBackground(
        connectionState = connectionState,
        isEmpty = bookmarks.isEmpty(),
        listContext = ListContext.BOOKMARKS,
        emptyRefreshableDesc = stringResource(R.string.no_bookmark_for_account),
        modifier = Modifier.padding(padding)
    ) {
        Box(Modifier.pullRefresh(state)) {
            when (listLayout) {
                ListLayout.GRID -> {
                    val listPadding = PaddingValues(
                        top = dimensionResource(R.dimen.margin_medium),
                        bottom = padding.calculateBottomPadding()
                            .plus(dimensionResource(R.dimen.margin)),
                        start = dimensionResource(R.dimen.margin_medium),
                        end = dimensionResource(R.dimen.margin_medium),
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
                        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_fab_padding)),
//                        contentPadding = padding,
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
                connectionState.loading == LoadingState.PROCESSING,
                state,
                Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
