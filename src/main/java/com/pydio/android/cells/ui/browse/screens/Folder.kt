package com.pydio.android.cells.ui.browse.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.TreeNodeLargeCard
import com.pydio.android.cells.ui.browse.composables.WrapWithActions
import com.pydio.android.cells.ui.browse.menus.SetMoreMenuState
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.MultiSelectTopBar
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpLargeGridItem
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpListItem
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private const val LOG_TAG = "Folder"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folder(
    isExpandedScreen: Boolean,
    folderID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    folderVM: FolderVM,
    browseHelper: BrowseHelper,
) {

    // UI States
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingState = browseRemoteVM.loadingState.collectAsState()
    val listLayout by folderVM.layout.collectAsState(ListLayout.LIST)
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val snackBarHostState = remember { SnackbarHostState() }
    val multiSelectData: MutableState<Set<StateID>> = rememberSaveable {
        mutableStateOf(setOf())
    }
    // State for the more Menus
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, Set<StateID>>> = remember {
        mutableStateOf(NodeMoreMenuType.NONE to setOf())
    }

    val setMoreMenuData: (NodeMoreMenuType, Set<StateID>) -> Unit = { t, ids ->
        Log.e(LOG_TAG, "set data for $t: $ids")
        nodeMoreMenuData.value = t to ids
    }

    // Business States
    val treeNode by folderVM.treeNode.collectAsState()
    val workspace by folderVM.workspace.collectAsState()
    val children = folderVM.children.collectAsState(listOf())
    val binLabel = stringResource(R.string.recycle_bin_label)
    val currNodeLabel by remember(key1 = treeNode, key2 = workspace) {
        derivedStateOf {
            var tmpLabel = folderID.fileName ?: workspace?.label ?: folderID.slug
            if (treeNode?.isRecycle() == true) {
                tmpLabel = binLabel
            }
            tmpLabel
        }
    }

    // Define specific functions

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(folderID, true)
    }

    val itemTapped: (StateID, Boolean) -> Unit = { stateID, longPress ->
        scope.launch {
            if (multiSelectData.value.isEmpty()) {
                if (longPress) { // Toggle to multi select mode and add the element
                    multiSelectData.value = setOf(stateID)
                } else { // short click
                    browseHelper.open(context, stateID, browseHelper.browse)
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

    val showFAB by remember(key1 = treeNode, key2 = multiSelectData.value.size) {
        derivedStateOf {
            val inRecycle = treeNode?.isRecycle() == true || treeNode?.isRecycle() == true
            val multiselectMode = multiSelectData.value.isNotEmpty()
            !inRecycle && !multiselectMode
        }
    }

    val openMoreMenu: (NodeMoreMenuType, Set<StateID>) -> Unit = { type, stateIDs ->
        scope.launch {
            setMoreMenuData(type, stateIDs)
            sheetState.expand()
        }
    }

    val moreMenuDone: () -> Unit = {
        scope.launch {
            sheetState.hide()
        }
        setMoreMenuData(NodeMoreMenuType.NONE, setOf())
        multiSelectData.value = setOf()
    }

    val actionDone: (Boolean) -> Unit = {
        moreMenuDone()
        if (it) { // Also reset backoff ticker
            browseRemoteVM.watch(folderID, true)
        }
    }

    val launch: (NodeAction) -> Unit = {
        when (it) {
            is NodeAction.AsGrid -> {
                folderVM.setListLayout(ListLayout.GRID)
                actionDone(true)
            }

            is NodeAction.AsList -> {
                folderVM.setListLayout(ListLayout.LIST)
                actionDone(true)
            }

            else -> {
                Log.e(LOG_TAG, "########### Unknown action: ${it.id}")
                actionDone(false)
            }
        }
    }

    WrapWithActions(
        actionDone = actionDone,
        isExpandedScreen = isExpandedScreen,
        type = nodeMoreMenuData.value.first,
        subjectIDs = nodeMoreMenuData.value.second,
        sheetState = sheetState,
        snackBarHostState = snackBarHostState,
    ) {
        FolderScaffold(
            isExpandedScreen = isExpandedScreen,
            loadingState = loadingState.value,
            listLayout = listLayout,
            showFAB = showFAB,
            label = currNodeLabel,
            stateID = folderID,
            children = children.value,
            forceRefresh = forceRefresh,
            openDrawer = openDrawer,
            openSearch = openSearch,
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
private fun FolderScaffold(
    isExpandedScreen: Boolean,
    loadingState: LoadingState,
    listLayout: ListLayout,
    showFAB: Boolean,
    label: String,
    stateID: StateID,
    children: List<TreeNodeItem>,
    forceRefresh: () -> Unit,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
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
                            moreMenuState.openMoreMenu(NodeMoreMenuType.MORE, selectedItems)
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
        floatingActionButton = {
            if (showFAB) {
                FloatingActionButton(onClick = {
                    moreMenuState.openMoreMenu(
                        NodeMoreMenuType.CREATE,
                        setOf(stateID)
                    )
                }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Open creation more menu"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding -> // Compulsory padding parameter. Must be applied to the topmost container/view in content:
        FolderList(
            loadingState = loadingState,
            listLayout = listLayout,
            isSelectionMode = selectedItems.isNotEmpty(),
            stateID = stateID,
            children = children,
            selectedItems = selectedItems,
            onTap = onTap,
            openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.MORE, setOf(it)) },
            forceRefresh = forceRefresh,
            padding = padding,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FolderList(
    loadingState: LoadingState,
    listLayout: ListLayout,
    isSelectionMode: Boolean,
    stateID: StateID,
    children: List<TreeNodeItem>,
    selectedItems: Set<StateID>,
    onTap: (StateID, Boolean) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    padding: PaddingValues,
) {
    // WARNING: pullRefresh API is:
    //   - experimental
    //   - only implemented in material "1" for the time being.
    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.d(LOG_TAG, "Force refresh launched")
        forceRefresh()
    })

    WithLoadingListBackground(
        loadingState = loadingState,
        isEmpty = children.isEmpty(),
        listContext = ListContext.BROWSE,
        canRefresh = LoadingState.SERVER_UNREACHABLE != loadingState,
        modifier = Modifier.padding(padding)
    ) {
        val alpha = getFloatResource(LocalContext.current, R.dimen.disabled_list_item_alpha)
        val parItemModifier = if (isSelectionMode) {
            Modifier
                .fillMaxWidth()
                .alpha(alpha)
        } else {
            Modifier
                .fillMaxWidth()
                .clickable { onTap(stateID.parent(), false) }
        }
        val parDesc = when {
            Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
            else -> stringResource(R.string.parent_folder)
        }

        Box(Modifier.pullRefresh(state)) {
            when (listLayout) {
                ListLayout.GRID -> {
                    val listPadding = PaddingValues(
                        top = dimensionResource(id = R.dimen.margin_medium),
                        bottom = padding.calculateBottomPadding()
                            .plus(dimensionResource(R.dimen.list_bottom_fab_padding)),
                        start = dimensionResource(id = R.dimen.margin_medium),
                        end = dimensionResource(id = R.dimen.margin_medium),
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_large_col_min_width)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                        contentPadding = listPadding,
                    ) {
                        if (Str.notEmpty(stateID.path)) {
                            item { M3BrowseUpLargeGridItem(parDesc, parItemModifier) }
                        }
                        items(
                            items = children,
                            key = { it.stateID.id }) { node ->
                            TreeNodeLargeCard(
                                item = node,
                                more = { openMoreMenu(node.defaultStateID()) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItems.contains(node.defaultStateID()),
                                modifier = getClickableModifier(isSelectionMode, node, onTap, alpha)
                                    .animateItemPlacement(),
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_fab_padding)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (Str.notEmpty(stateID.path)) {
                            item(key = "parent") { M3BrowseUpListItem(parDesc, parItemModifier) }
                        }
                        items(children, key = { it.stateID.id }) { node ->
                            NodeItem(
                                item = node,
                                title = getNodeTitle(name = node.name, mime = node.mime),
                                desc = getNodeDesc(
                                    node.remoteModTs,
                                    node.size,
                                    node.localModStatus
                                ),
                                more = { openMoreMenu(node.stateID) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedItems.contains(node.defaultStateID()),
                                modifier = getClickableModifier(isSelectionMode, node, onTap, alpha)
                                    .animateItemPlacement()
                            )
                        }
                    }
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

// Tweak the clickable behaviour depending on current node and selection mode.
@SuppressLint("ModifierFactoryExtensionFunction")
@OptIn(ExperimentalFoundationApi::class)
fun getClickableModifier(
    isSelectionMode: Boolean,
    item: TreeNodeItem,
    onTap: (StateID, Boolean) -> Unit,
    alpha: Float,
): Modifier {
    var tmpModifier = Modifier.fillMaxWidth()
    tmpModifier = if (item.isRecycle) {
        if (isSelectionMode) {
            // Do not react to click and make less visible
            tmpModifier.alpha(alpha)
        } else { // Recycle bin does not support multi selection
            tmpModifier.clickable { onTap(item.defaultStateID(), false) }
        }
    } else {
        tmpModifier.combinedClickable(
            onClick = { onTap(item.defaultStateID(), false) },
            onLongClick = { onTap(item.defaultStateID(), true) },
        )
    }
    return tmpModifier
}


//@Composable
//fun FolderTopBar(
//    title: String,
//    openDrawer: () -> Unit,
//    openSearch: () -> Unit,
//    modifier: Modifier
//) {
//    Surface(
//        modifier = modifier
//    ) {
//        Row(
//            Modifier
//                .fillMaxWidth()
//                .padding(
//                    horizontal = dimensionResource(R.dimen.topbar_horizontal_padding),
//                    vertical = dimensionResource(R.dimen.topbar_vertical_padding),
//                )
//        ) {
//            IconButton(
//                onClick = { openDrawer() },
//                enabled = true
//            ) {
//                Icon(
//                    CellsIcons.Menu,
//                    contentDescription = stringResource(id = R.string.open_drawer)
//                )
//            }
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = title,
//                    style = MaterialTheme.typography.titleMedium,
//                )
//            }
//            IconButton(onClick = { openSearch() }) {
//                Icon(
//                    CellsIcons.Search,
//                    contentDescription = stringResource(id = R.string.action_search)
//                )
//            }
//        }
//    }
//}
//
//@Preview(name = "Folder Header Light Mode")
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "FolderTopBar Dark Mode"
//)
//@Composable
//private fun FolderTopBarPreview() {
//    UseCellsTheme {
//        FolderTopBar(
//            "alice",
//            { },
//            { },
//            Modifier.fillMaxWidth()
//        )
//    }
//}
//
//@Preview(name = "Light Mode")
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "Dark Mode"
//)
//@Composable
//private fun TopBarPreview() {
//    UseCellsTheme {
//        FolderTopBar(
//            "Pydio Cells server",
//            { },
//            { },
//            Modifier.fillMaxWidth()
//        )
//    }
//}
