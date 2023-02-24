package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.ui.browse.composables.OfflineRootItem
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.models.OfflineVM
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.LoadingState
import com.pydio.cells.transport.StateID

private const val logTag = "OfflineRoots.kt"

@Composable
fun OfflineRoots(
    accountID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    offlineVM: OfflineVM,
) {
    val loadingState by browseRemoteVM.loadingState.observeAsState()

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(accountID, true)
    }

    val roots = offlineVM.offlineRoots.observeAsState()

    val openInWorkspaces: (StateID) -> Unit = {
        // TODO
        // put the checks to see what we open here
    }

    OfflineScaffold(
        loadingState = loadingState ?: LoadingState.STARTING,
        title = stringResource(id = R.string.action_open_offline_roots),
        roots = roots.value ?: listOf(),
        openDrawer = openDrawer,
        openSearch = openSearch,
        forceRefresh = forceRefresh,
        openInWorkspaces = openInWorkspaces,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineScaffold(
    loadingState: LoadingState,
    title: String,
    roots: List<RLiveOfflineRoot>,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    forceRefresh: () -> Unit,
    openInWorkspaces: (StateID) -> Unit,
) {
    Scaffold(
        topBar = {
            DefaultTopBar(
                title = title,
                openDrawer = openDrawer,
                openSearch = openSearch,
            )
        },
    ) { padding ->
        OfflineRootList(
            loadingState = loadingState,
            roots = roots,
            forceRefresh = forceRefresh,
            open = { }, // TODO
            openMoreMenu = { }, // TODO
            padding = padding,
            modifier = Modifier.fillMaxWidth(), // padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OfflineRootList(
    loadingState: LoadingState,
    roots: List<RLiveOfflineRoot>,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
) {

    val context = LocalContext.current

    val state = rememberPullRefreshState(
        loadingState == LoadingState.PROCESSING,
        onRefresh = { Log.i(logTag, "Force refresh launched");forceRefresh() },
    )

    Box(modifier.pullRefresh(state)) {
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(roots) { node ->


                OfflineRootItem(
                    item = node,
                    title = getNodeTitle(name = node.name, mime = node.mime),
                    desc = "Implement me",
//                    desc = getNodeDesc(
//                        context,
//                        node.remoteModificationTS,
//                        node.size,
//                        node.localModificationStatus
//                    ),
                    more = { openMoreMenu(node.getStateID()) },
                    modifier = Modifier.clickable { open(node.getStateID()) },
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
