package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.WrapWithActions
import com.pydio.android.cells.ui.browse.menus.MoreMenuState
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithIcon
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithThumb
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpLargeGridItem
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpListItem
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private const val logTag = "Folder"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folder(
    folderID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    folderVM: FolderVM,
    browseHelper: BrowseHelper,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadingState by browseRemoteVM.loadingState.observeAsState()
    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(folderID, true)
    }

    val listLayout by folderVM.layout.collectAsState(ListLayout.LIST)

    val treeNode by folderVM.treeNode.collectAsState()
    val workspace by folderVM.workspace.collectAsState()
    val children by folderVM.childNodes.observeAsState()

    val binLabel = stringResource(R.string.recycle_bin_label)

    val label by remember(key1 = treeNode, key2 = workspace) {
        derivedStateOf {
            var tmpLabel = folderID.fileName ?: workspace?.label ?: folderID.workspace
            if (treeNode?.isRecycle() == true) {
                tmpLabel = binLabel
            }
            tmpLabel
        }
    }

    val showFAB by remember(key1 = treeNode) {
        derivedStateOf {
            val inRecycle = treeNode?.isRecycle() == true || treeNode?.isRecycle() == true
            !inRecycle
        }
    }

    // State for the more Menus
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, StateID>> = remember {
        mutableStateOf(
            Pair(
                NodeMoreMenuType.NONE,
                StateID.NONE
            )
        )
    }

    val openMoreMenu: (NodeMoreMenuType, StateID) -> Unit = { type, currID ->
        scope.launch {
            Log.d(logTag, "About to open $type more menu for $currID")
            nodeMoreMenuData.value = Pair(type, currID)
            sheetState.expand()
        }
    }

    val localOpen: (StateID) -> Unit = {
        scope.launch {
            browseHelper.open(context, it)
        }
    }

    val actionDone: (Boolean) -> Unit = {
        scope.launch {
            if (it) { // Also reset backoff ticker
                Log.d(logTag, "Action done for $folderID")
                browseRemoteVM.watch(folderID, true)
            }
            sheetState.hide()
            nodeMoreMenuData.value = Pair(
                NodeMoreMenuType.NONE,
                StateID.NONE
            )
        }
    }

    val launch: (NodeAction, StateID) -> Unit = { action, currID ->
        when (action) {
            is NodeAction.AsGrid -> {
                folderVM.setListLayout(ListLayout.GRID)
                actionDone(true)
            }

            is NodeAction.AsList -> {
                folderVM.setListLayout(ListLayout.LIST)
                actionDone(true)
            }

            is NodeAction.SortBy -> { // The real set has already been done by the bottom sheet via its preferencesVM
                actionDone(true)
            }

            else -> {
                Log.e(logTag, "Unknown action $action for $currID")
                actionDone(false)
            }
        }
    }

    WrapWithActions(
        actionDone = actionDone,
        type = nodeMoreMenuData.value.first,
        toOpenStateID = nodeMoreMenuData.value.second,
        sheetState = sheetState,
    ) {
        FolderScaffold(
            loadingState = loadingState ?: LoadingState.STARTING,
            listLayout = listLayout,
            showFAB = showFAB,
            label = label,
            stateID = folderID,
            children = children ?: listOf(),
            forceRefresh = forceRefresh,
            openDrawer = openDrawer,
            openSearch = openSearch,
            openParent = { localOpen(folderID.parent()) },
            open = localOpen,
            launch = launch,
            moreMenuState = MoreMenuState(
                nodeMoreMenuData.value.first,
                sheetState,
                nodeMoreMenuData.value.second,
                openMoreMenu
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderScaffold(
    loadingState: LoadingState,
    listLayout: ListLayout,
    showFAB: Boolean,
    label: String,
    stateID: StateID,
    children: List<RTreeNode>,
    forceRefresh: () -> Unit,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    launch: (NodeAction, StateID) -> Unit,
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
            val btnLabel = stringResource(R.string.button_switch_to_list_layout)
            DropdownMenuItem(
                text = { Text(btnLabel) },
                onClick = {
                    launch(
                        NodeAction.AsList,
                        StateID.NONE
                    )
                    showMenu(false)
                },
                leadingIcon = { Icon(CellsIcons.AsList, btnLabel) },
            )
        } else {
            val btnLabel = stringResource(R.string.button_switch_to_grid_layout)
            DropdownMenuItem(
                text = { Text(btnLabel) },
                onClick = {
                    launch(
                        NodeAction.AsGrid,
                        StateID.NONE
                    )
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
                    stateID
                )
                showMenu(false)
            },
            leadingIcon = { Icon(CellsIcons.SortBy, btnLabel) },
        )
    }

    Scaffold(
        topBar = {
            TopBarWithMoreMenu(
                title = label,
                openDrawer = openDrawer,
                openSearch = openSearch,
                isActionMenuShown = isShown,
                showMenu = showMenu,
                content = actionMenuContent
            )
        },
        floatingActionButton = {
            if (showFAB) {
                FloatingActionButton(onClick = {
                    moreMenuState.openMoreMenu(
                        NodeMoreMenuType.CREATE,
                        stateID
                    )
                }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Open creation more menu"
                    )
                }
            }
        },
    ) { padding -> // Compulsory padding parameter. Must be applied to the topmost container/view in content:
        FolderList(
            loadingState = loadingState,
            listLayout = listLayout,
            stateID = stateID,
            children = children,
            openParent = openParent,
            open = open,
            openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.MORE, it) },
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
    stateID: StateID,
    children: List<RTreeNode>,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    padding: PaddingValues,
) {
    // WARNING: pullRefresh API is:
    //   - experimental
    //   - only implemented in material "1" for the time being.
    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.d(logTag, "Force refresh launched")
        forceRefresh()
    })

    WithLoadingListBackground(
        loadingState = loadingState,
        isEmpty = children.isEmpty(),
        listContext = ListContext.BROWSE,
        // TODO also handle if server is unreachable
        canRefresh = true,
        modifier = Modifier.padding(padding)
    ) {
        Box(
            Modifier
                //   .fillMaxSize()
                .pullRefresh(state)
        ) {
            when (listLayout) {
                ListLayout.GRID -> {
                    val listPadding = PaddingValues(
                        top = dimensionResource(id = R.dimen.margin_medium), // padding.calculateTopPadding(),
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
                            item {
                                val parentDescription = when {
                                    Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
                                    else -> stringResource(R.string.parent_folder)
                                }
                                M3BrowseUpLargeGridItem(
                                    parentDescription,
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { openParent(stateID) }
                                )
                            }
                        }
                        items(children, key = { it.encodedState }) { node ->
                            if (node.isPreViewable() && !node.hasThumb()) {
                                Log.e(logTag, "Previewable, **NO** thumb")
                            } else if (node.hasThumb()) {
                                // TODO this log is important so that we are "notified" when the meta-hash changes
                                Log.d(logTag, "Thumb for ${node.name}, M-Hash: ${node.metaHash}")
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
                                    openMoreMenu = { openMoreMenu(node.getStateID()) },
                                    sortName = node.sortName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { open(node.getStateID()) }
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
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.list_bottom_fab_padding)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (Str.notEmpty(stateID.path)) {
                            item(key = "parent") {
                                val parentDescription = when {
                                    Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
                                    else -> stringResource(R.string.parent_folder)
                                }
                                M3BrowseUpListItem(
                                    parentDescription = parentDescription,
                                    modifier = Modifier.clickable { openParent(stateID) }
                                )
                            }
                        }
                        items(children, key = { it.encodedState }) { node ->
                            // TODO clean this, see above
                            Log.d(logTag, "Thumb for ${node.name}, M-Hash: ${node.metaHash}")
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
                                    .padding(0.dp)
                                    .fillMaxWidth()
                                    .clickable { open(node.getStateID()) }
                                    // This breaks the layout and makes the trailing buttons disappear
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

@Composable
fun FolderTopBar(
    title: String,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.topbar_horizontal_padding),
                    vertical = dimensionResource(R.dimen.topbar_vertical_padding),
                )
        ) {
            IconButton(
                onClick = { openDrawer() },
                enabled = true
            ) {
                Icon(
                    CellsIcons.Menu,
                    contentDescription = stringResource(id = R.string.open_drawer)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            IconButton(onClick = { openSearch() }) {
                Icon(
                    CellsIcons.Search,
                    contentDescription = stringResource(id = R.string.action_search)
                )
            }
        }
    }
}

@Preview(name = "Folder Header Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "FolderTopBar Dark Mode"
)
@Composable
private fun FolderTopBarPreview() {
    UseCellsTheme {
        FolderTopBar(
            "alice",
            { },
            { },
            Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun TopBarPreview() {
    UseCellsTheme {
        FolderTopBar(
            "Pydio Cells server",
            { },
            { },
            Modifier.fillMaxWidth()
        )
    }
}
