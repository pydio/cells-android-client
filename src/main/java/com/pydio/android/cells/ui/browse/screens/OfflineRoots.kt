package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.text.format.Formatter
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.browse.composables.OfflineRootGridItem
import com.pydio.android.cells.ui.browse.composables.OfflineRootItem
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.menus.MoreMenuState
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.TopBarWithMoreMenu
import com.pydio.android.cells.ui.core.composables.WithLoadingListBackground
import com.pydio.android.cells.ui.core.composables.animations.SmoothLinearProgressIndicator
import com.pydio.android.cells.ui.core.composables.getJobStatus
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.core.composables.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.asAgoString
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val logTag = "OfflineRoots"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRoots(
    offlineVM: OfflineVM,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadingState by offlineVM.loadingState.observeAsState()
    val currJob = offlineVM.syncJob.observeAsState()
    val listLayout by offlineVM.layout.observeAsState()
    val roots = offlineVM.offlineRoots.observeAsState()


    val localOpen: (StateID) -> Unit = { stateID ->
        scope.launch {
            offlineVM.getNode(stateID)?.let {
                if (it.isFolder()) {
                    open(stateID)
//                } else if (it.isPreViewable()) {
                    // TODO (since v2) Open carousel for offline nodes
                } else {
                    offlineVM.viewFile(context, stateID)
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val nodeMoreMenuData: MutableState<Pair<NodeMoreMenuType, StateID>> = remember {
        mutableStateOf(Pair(NodeMoreMenuType.BOOKMARK, Transport.UNDEFINED_STATE_ID))
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
            nodeMoreMenuData.value = Pair(NodeMoreMenuType.BOOKMARK, Transport.UNDEFINED_STATE_ID)
        }
    }

    val destinationPicker = rememberLauncherForActivityResult(
        // TODO we have the mime of the file to download to device
        //    but this is no trivial implementation: the contract must then be both
        //    dynamic AND remembered.
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            if (nodeMoreMenuData.value.second != Transport.UNDEFINED_STATE_ID) {
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
                            open(stateID)
                        } else {
                            open(stateID.parent())
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

    OfflineScaffold(
        loadingState = loadingState ?: LoadingState.STARTING,
        listLayout = listLayout ?: ListLayout.LIST,
        runningJob = currJob.value,
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
private fun OfflineScaffold(
    loadingState: LoadingState,
    listLayout: ListLayout,
    runningJob: RJob?,
    title: String,
    roots: List<RLiveOfflineRoot>,
    openDrawer: () -> Unit,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    launch: (NodeAction, StateID) -> Unit,
    moreMenuState: MoreMenuState,
) {

    val tint = MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface

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
                    launch(NodeAction.AsList, Transport.UNDEFINED_STATE_ID)
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
                    launch(NodeAction.AsGrid, Transport.UNDEFINED_STATE_ID)
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
                    Transport.UNDEFINED_STATE_ID
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

        ModalBottomSheetLayout(
            sheetContent = {
                if (moreMenuState.type == NodeMoreMenuType.SORT_BY) {
                    SortByMenu(
                        done = { launch(NodeAction.SortBy, Transport.UNDEFINED_STATE_ID) },
                        tint = tint,
                        bgColor = bgColor,
                    )
                } else {
                    NodeMoreMenuData(
                        type = NodeMoreMenuType.OFFLINE,
                        toOpenStateID = moreMenuState.stateID,
                        launch = { launch(it, moreMenuState.stateID) },
                        tint = tint,
                        bgColor = bgColor,
                    )
                }
            },
            modifier = Modifier,
            sheetState = moreMenuState.sheetState,
            sheetBackgroundColor = bgColor,
        ) {

            OfflineRootsList(
                loadingState = loadingState,
                listLayout = listLayout,
                runningJob = runningJob,
                roots = roots,
                forceRefresh = forceRefresh,
                openMoreMenu = { moreMenuState.openMoreMenu(NodeMoreMenuType.OFFLINE, it) },
                open = open,
                padding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding().plus(dimensionResource(R.dimen.margin_medium)),
                    start = dimensionResource(R.dimen.list_horizontal_padding),
                    end = dimensionResource(R.dimen.list_horizontal_padding),
                ),
                modifier = Modifier.fillMaxWidth(), // padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OfflineRootsList(
    loadingState: LoadingState,
    listLayout: ListLayout,
    runningJob: RJob?,
    roots: List<RLiveOfflineRoot>,
    forceRefresh: () -> Unit,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
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
        // TODO also handle if server is unreachable
        canRefresh = true,
        modifier = Modifier.fillMaxSize()
    ) {

        Box(modifier.pullRefresh(state)) {
            when (listLayout) {
                ListLayout.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_large_col_min_width)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_col_spaced_by)),
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_col_spaced_by)),
                        contentPadding = padding,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // FIXME this is not yet done:
                        //  - prepare offline node item composable for grids
                        //  - re-enable option in the action menu
                        if (runningJob != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val percentage =
                                    (runningJob.progress).toFloat().div(runningJob.total)
                                SyncStatus(
                                    desc = getJobStatus(item = runningJob),
                                    progress = percentage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        items(roots) { offlineRoot ->
                            OfflineRootGridItem(
                                item = offlineRoot,
                                title = getNodeTitle(
                                    name = offlineRoot.name,
                                    mime = offlineRoot.mime
                                ),
                                desc = getDesc(offlineRoot),
                                more = { openMoreMenu(offlineRoot.getStateID()) },
                                modifier = Modifier.clickable { open(offlineRoot.getStateID()) },
                            )
                        }
                    }
                }
                else -> {

                    LazyColumn(
                        contentPadding = padding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (runningJob != null) {
                            item {
                                val percentage =
                                    (runningJob.progress).toFloat().div(runningJob.total)
                                SyncStatus(
                                    desc = getJobStatus(item = runningJob),
                                    progress = percentage,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        items(roots) { offlineRoot ->
                            OfflineRootItem(
                                item = offlineRoot,
                                title = getNodeTitle(
                                    name = offlineRoot.name,
                                    mime = offlineRoot.mime
                                ),
                                desc = getDesc(offlineRoot),
                                more = { openMoreMenu(offlineRoot.getStateID()) },
                                modifier = Modifier.clickable { open(offlineRoot.getStateID()) },
                            )
                        }
                        item {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimensionResource(R.dimen.recycler_bottom_fab_padding))
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
    CellsTheme {
        SyncStatus(
            "Pydio Cells server",
            -1f,
            Modifier.fillMaxWidth()
        )
    }
}
