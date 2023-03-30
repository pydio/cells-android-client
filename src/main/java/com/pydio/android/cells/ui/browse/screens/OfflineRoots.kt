package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.text.format.Formatter
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.ListType
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.OfflineRootItem
import com.pydio.android.cells.ui.browse.menus.MoreMenuState
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.animations.SmoothLinearProgressIndicator
import com.pydio.android.cells.ui.core.composables.getJobStatus
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithIcon
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithThumb
import com.pydio.android.cells.ui.core.composables.lists.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.menus.CellsModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.android.cells.utils.asAgoString
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val logTag = "OfflineRoots"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRoots(
    offlineVM: OfflineVM,
    openDrawer: () -> Unit,
    browseHelper: BrowseHelper,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadingState by offlineVM.loadingState.observeAsState()
    val syncJob = offlineVM.syncJob.observeAsState()
    val listLayout by offlineVM.layout.collectAsState(ListLayout.LIST)
    val roots = offlineVM.offlineRoots.observeAsState()

    val localOpen: (StateID) -> Unit = { stateID ->
        scope.launch {
            offlineVM.getNode(stateID)?.let {
                if (it.isFolder()) {
                    browseHelper.open(context, stateID)
                } else {
                    browseHelper.open(context, stateID)
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, StateID>> = remember {
        mutableStateOf(
            Pair(
                NodeMoreMenuType.OFFLINE,
                StateID.NONE
            )
        )
    }
    val openMoreMenu: (NodeMoreMenuType, StateID) -> Unit = { type, stateID ->
        scope.launch {
            nodeMoreMenuData.value = Pair(type, stateID)
            sheetState.expand()
        }
    }

    val moreMenuDone: () -> Unit = {
        scope.launch {
            sheetState.hide()
            nodeMoreMenuData.value = Pair(
                NodeMoreMenuType.BOOKMARK,
                StateID.NONE
            )
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
                    offlineVM.download(nodeMoreMenuData.value.second, uri)
                }
            }
            moreMenuDone()
        }
    )

    val launch: (NodeAction, StateID) -> Unit = { action, stateID ->
        when (action) {
            is NodeAction.OpenInApp -> {
                moreMenuDone()
                scope.launch {
                    offlineVM.getNode(stateID)?.let {
                        if (it.isFolder()) {
                            localOpen(stateID)
                        } else {
                            localOpen(stateID.parent())
                        }
                    }
                }
            }
            is NodeAction.ForceResync -> {
                offlineVM.forceSync(stateID)
                moreMenuDone()
            }
            is NodeAction.DownloadToDevice -> {
                destinationPicker.launch(stateID.fileName)
                // Done is called by the destination picker callback
            }
            is NodeAction.ToggleOffline -> {
                offlineVM.removeFromOffline(stateID)
                moreMenuDone()
            }
            is NodeAction.AsGrid -> {
                offlineVM.setListLayout(ListLayout.GRID)
                moreMenuDone()
            }
            is NodeAction.AsList -> {
                offlineVM.setListLayout(ListLayout.LIST)
                moreMenuDone()
            }
            else -> {
                Log.e(logTag, "Unknown action $action for $stateID")
                moreMenuDone()
            }
        }
    }

    WithScaffold(
        loadingState = loadingState ?: LoadingState.STARTING,
        listLayout = listLayout,
        syncJob = syncJob.value,
        title = stringResource(id = R.string.action_open_offline_roots),
        roots = roots.value ?: listOf(),
        openDrawer = openDrawer,
        forceRefresh = offlineVM::forceFullSync,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    loadingState: LoadingState,
    listLayout: ListLayout,
    syncJob: RJob?,
    title: String,
    roots: List<RLiveOfflineRoot>,
    openDrawer: () -> Unit,
    forceRefresh: () -> Unit,
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
            DropdownMenuItem(
                text = { Text(stringResource(R.string.button_switch_to_list_layout)) },
                onClick = {
                    launch(
                        NodeAction.AsList,
                        StateID.NONE
                    )
                    showMenu(false)
                },
                leadingIcon = {
                    Icon(
                        CellsIcons.AsList,
                        stringResource(R.string.button_switch_to_list_layout)
                    )
                },
            )
        } else {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.button_switch_to_grid_layout)) },
                onClick = {
                    launch(
                        NodeAction.AsGrid,
                        StateID.NONE
                    )
                    showMenu(false)
                },
                leadingIcon = {
                    Icon(
                        CellsIcons.AsGrid,
                        stringResource(R.string.button_switch_to_grid_layout)
                    )
                },
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
            TopBarWithMoreMenu(
                title = title,
                openDrawer = openDrawer,
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
                        type = NodeMoreMenuType.OFFLINE,
                        toOpenStateID = moreMenuState.stateID,
                        launch = { launch(it, moreMenuState.stateID) },
                    )
                }
            },
            sheetState = moreMenuState.sheetState,
        ) {
            OfflineRootsList(
                loadingState = loadingState,
                listLayout = listLayout,
                syncJob = syncJob,
                roots = roots,
                forceRefresh = forceRefresh,
                openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.OFFLINE, it) },
                open = open,
                padding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                        .plus(dimensionResource(R.dimen.margin_medium)),
                    start = dimensionResource(R.dimen.list_horizontal_padding),
                    end = dimensionResource(R.dimen.list_horizontal_padding),
                ),
            )
        }
    }
}

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun OfflineRootsList(
    loadingState: LoadingState,
    listLayout: ListLayout,
    syncJob: RJob?,
    roots: List<RLiveOfflineRoot>,
    forceRefresh: () -> Unit,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit,
    padding: PaddingValues,
) {

    val state = rememberPullRefreshState(
        refreshing = loadingState == LoadingState.PROCESSING,
        onRefresh = {
            Log.e(logTag, "Force refresh launched")
            forceRefresh()
        },
    )

    WithLoadingListBackground(
        loadingState = loadingState,
        isEmpty = roots.isEmpty(),
        canRefresh = true,
        emptyRefreshableDesc = stringResource(id = R.string.no_offline_root_for_account),
        modifier = Modifier.fillMaxSize()
    ) {

        Box(Modifier.pullRefresh(state)) { // .fillMaxSize()) {
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
                        if (syncJob != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val percentage =
                                    (syncJob.progress).toFloat().div(syncJob.total)
                                SyncStatus(
                                    desc = getJobStatus(item = syncJob),
                                    progress = percentage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        items(
                            items = roots,
                            key = { it.encodedState }) { node ->
                            if (node.hasThumb()) {
                                LargeCardWithThumb(
                                    stateID = node.getStateID(),
                                    eTag = node.etag,
                                    title = getNodeTitle(name = node.name, mime = node.mime),
                                    desc = getDesc(node),
                                    openMoreMenu = { openMoreMenu(node.getStateID()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { open(node.getStateID()) }
                                        .animateItemPlacement(),
                                )
                            } else {
                                LargeCardWithIcon(
                                    sortName = node.sortName,
                                    mime = node.mime,
                                    title = getNodeTitle(name = node.name, mime = node.mime),
                                    desc = getDesc(node),
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
                        if (syncJob != null) {
                            item {
                                val percentage =
                                    (syncJob.progress).toFloat().div(syncJob.total)
                                SyncStatus(
                                    desc = getJobStatus(item = syncJob),
                                    progress = percentage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        items(roots, key = { it.encodedState }) { offlineRoot ->
                            OfflineRootItem(
                                item = offlineRoot,
                                title = getNodeTitle(
                                    name = offlineRoot.name,
                                    mime = offlineRoot.mime
                                ),
                                desc = getDesc(offlineRoot),
                                more = { openMoreMenu(offlineRoot.getStateID()) },
                                modifier = Modifier
                                    .clickable { open(offlineRoot.getStateID()) }
                                    .animateItemPlacement(),
                            )
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimensionResource(R.dimen.list_bottom_fab_padding))
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = loadingState == LoadingState.PROCESSING,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun SyncStatus(
    desc: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(R.dimen.list_item_inner_padding))
        ) {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (progress == -1f) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(id = R.dimen.margin_small))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            } else if (progress in 0.0..1.0) {
                SmoothLinearProgressIndicator(
                    indicatorProgress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(id = R.dimen.margin_small))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun getDesc(item: RLiveOfflineRoot): String {
    val context = LocalContext.current
    val prefix = stringResource(R.string.last_check)
    val mTimeValue = if (item.lastCheckTs > 1) {
        asAgoString(item.lastCheckTs)
    } else {
        stringResource(R.string.last_check_never)
    }
    val sizeValue = Formatter.formatShortFileSize(context, item.size)
    return "$prefix: $mTimeValue • $sizeValue"
}

@Preview(name = "SyncStatus Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "SyncStatus Dark Mode"
)
@Composable
private fun SyncStatusPreview() {
    UseCellsTheme {
        SyncStatus(
            "Pydio Cells server",
            -1f,
            Modifier.fillMaxWidth()
        )
    }
}
