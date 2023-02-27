package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.browse.composables.MoreMenuType
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.OfflineRootItem
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.models.MoreMenuVM
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.core.composables.getJobStatus
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.asAgoString
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val logTag = "OfflineRoots.kt"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRoots(
    accountID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
    offlineVM: OfflineVM,
) {

    val scope = rememberCoroutineScope()
    val loadingState by offlineVM.loadingState.observeAsState()

    val roots = offlineVM.offlineRoots.observeAsState()
    val currJob = offlineVM.syncJob.observeAsState()

    val localOpen: (StateID) -> Unit = {
        // TODO
        open(it)
        // put the checks to see what we open here
    }

    val moreMenuState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val moreMenuData: MutableState<StateID> = remember {
        mutableStateOf(Transport.UNDEFINED_STATE_ID)
    }

    val openMoreMenu: (StateID) -> Unit = { stateID ->
        scope.launch {
            moreMenuData.value = stateID
            moreMenuState.expand()
        }
    }

    val moreMenuDone: () -> Unit = {
        scope.launch {
            moreMenuState.hide()
            moreMenuData.value = Transport.UNDEFINED_STATE_ID
        }
    }

    OfflineScaffold(
        loadingState = loadingState ?: LoadingState.STARTING,
        runningJob = currJob.value,
        title = stringResource(id = R.string.action_open_offline_roots),
        roots = roots.value ?: listOf(),
        openDrawer = openDrawer,
        openSearch = openSearch,
        moreMenu = Triple(moreMenuState, moreMenuData.value, openMoreMenu),
        moreMenuDone = moreMenuDone,
        forceRefresh = offlineVM::forceRefresh,
        open = localOpen,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineScaffold(
    loadingState: LoadingState,
    runningJob: RJob?,
    title: String,
    roots: List<RLiveOfflineRoot>,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    moreMenu: Triple<ModalBottomSheetState, StateID, (StateID) -> Unit>,
    moreMenuDone: () -> Unit,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
) {

    val moreMenuVM: MoreMenuVM = koinViewModel()

    val launch: (NodeAction) -> Unit = { it ->
        when (it) {
            is NodeAction.OpenParentLocation -> {
                Log.e(logTag, "#### IMPLEMENT NodeAction.OpenParentLocation")
                moreMenuDone()
            }
            is NodeAction.ForceResync -> {
                Log.e(logTag, "#### IMPLEMENT NodeAction.ForceResync")
                moreMenuDone()
            }
            else -> {
                Log.e(logTag, "#### UNKNOWN ACTION")
                moreMenuDone()
            }
        }
    }

    Scaffold(
        topBar = {
            DefaultTopBar(
                title = title,
                openDrawer = openDrawer,
                openSearch = openSearch,
            )
        },
    ) { padding ->

        ModalBottomSheetLayout(
            sheetContent = {
                NodeMoreMenuData(
                    MoreMenuType.OFFLINE,
                    moreMenu.second,
                    launch,
                    moreMenuVM,
                    MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.colorScheme.surface,
                )
            },
            modifier = Modifier,
            sheetState = moreMenu.first,
//            sheetBackgroundColor = bgColor,
        ) {
            OfflineRootList(
                loadingState = loadingState,
                runningJob = runningJob,
                roots = roots,
                forceRefresh = forceRefresh,
                open = open,
                openMoreMenu = moreMenu.third,
                padding = padding,
                modifier = Modifier.fillMaxWidth(), // padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OfflineRootList(
    loadingState: LoadingState,
    runningJob: RJob?,
    roots: List<RLiveOfflineRoot>,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
) {

    val context = LocalContext.current

    val state = rememberPullRefreshState(
        refreshing = loadingState == LoadingState.PROCESSING,
        onRefresh = {
            Log.e(logTag, "Force refresh launched")
            forceRefresh()
        },
    )

    Box(modifier.pullRefresh(state)) {
//        Column {
//            if (runningJob != null) {
//                SyncStatus(
//                    desc = getJobStatus(item = runningJob),
//                    progress = -1f,
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (runningJob != null) {
                item {
                    val percentage = (runningJob.progress * 100).toFloat().div(runningJob.total)
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
                    title = getNodeTitle(name = offlineRoot.name, mime = offlineRoot.mime),
                    desc = getDesc(offlineRoot),
                    more = { openMoreMenu(offlineRoot.getStateID()) },
                    modifier = Modifier.clickable { open(offlineRoot.getStateID()) },
                )
            }
        }
        if (runningJob != null) {
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
//    title: String,
//    desc: AnnotatedString,
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
                LinearProgressIndicator()
            } else if (progress >= 0 && progress < 1) {
                LinearProgressIndicator(progress = progress)
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
