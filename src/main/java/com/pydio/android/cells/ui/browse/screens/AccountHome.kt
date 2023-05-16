package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.android.cells.ui.browse.models.AccountHomeVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.core.composables.MainTitleText
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithIcon
import com.pydio.android.cells.ui.core.composables.lists.WithListTheme
import com.pydio.android.cells.ui.core.nav.AccountHeader
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

private const val logTag = "AccountHome"

@Composable
fun AccountHome(
    accountID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    accountHomeVM: AccountHomeVM,
    browseHelper: BrowseHelper,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val loadingState = browseRemoteVM.loadingState.collectAsState()
    val sessionView by accountHomeVM.currSession.observeAsState()
    val workspaces by accountHomeVM.wss.observeAsState()
    val cells by accountHomeVM.cells.observeAsState()

    val title = sessionView?.serverLabel() ?: "${accountID.serverUrl} - Home"

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(accountID, true)
    }

    WithScaffold(
        stateID = accountID,
        title = title,
        workspaces = workspaces ?: listOf(),
        cells = cells ?: listOf(),
        loadingState = loadingState.value,
        openDrawer = openDrawer,
        openAccounts = {
            scope.launch {
                browseHelper.open(context, StateID.NONE)
            }
        },
        openWorkspace = {
            scope.launch {
                browseHelper.open(context, it)
            }
        },
        openSearch = openSearch,
        forceRefresh = forceRefresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithScaffold(
    stateID: StateID,
    title: String,
    workspaces: List<RWorkspace>,
    cells: List<RWorkspace>,
    loadingState: LoadingState,
    openDrawer: () -> Unit,
    openAccounts: () -> Unit,
    openWorkspace: (StateID) -> Unit,
    openSearch: () -> Unit,
    forceRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            DefaultTopBar(
                title = title,
                openDrawer = openDrawer,
                openSearch = openSearch,
            )
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:

//        Log.e(logTag, "### About to create the list passed content padding")
//        Log.e(logTag, "$padding")

        val listPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding().plus(dimensionResource(R.dimen.margin)),
            start = dimensionResource(id = R.dimen.margin_medium),
            end = dimensionResource(id = R.dimen.margin_medium),
        )

        HomeListContent(
            loadingState = loadingState,
            stateID = stateID,
            workspaces = workspaces,
            cells = cells,
            open = openWorkspace,
            openAccounts = openAccounts,
            forceRefresh = forceRefresh,
            padding = listPadding,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun HomeListContent(
    loadingState: LoadingState,
    stateID: StateID,
    workspaces: List<RWorkspace>,
    cells: List<RWorkspace>,
    open: (StateID) -> Unit,
    openAccounts: () -> Unit,
    forceRefresh: () -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
) {

    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh()
    })

    Box(modifier.pullRefresh(state)) {
        WithListTheme {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_large_col_min_width)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.grid_large_padding)),
                contentPadding = padding,
            ) {

                item(span = {
                    GridItemSpan(maxLineSpan)
                }) {
                    AccountHeader(
                        username = stateID.username,
                        address = stateID.serverUrl,
                        openAccounts = openAccounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(id = R.dimen.margin_small))
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                        thickness = 0.3.dp,
                    )
                }
                if (workspaces.isNotEmpty()) {
                    item(span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        MainTitleText(
                            text = stringResource(R.string.category_workspaces),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(workspaces, key = { it.encodedState }) { ws ->
                        LargeCardWithIcon(
                            sortName = ws.sortName,
                            title = ws.label ?: "",
                            desc = ws.description ?: "",
                            mime = ws.type,
                            modifier = Modifier
                                .wrapContentSize(align = Alignment.Center)
                                .clickable { open(ws.getStateID()) },
                        )
                    }
                }

                if (cells.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MainTitleText(
                            text = stringResource(R.string.category_cells),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(cells, key = { it.encodedState }) { ws ->
                        LargeCardWithIcon(
                            sortName = ws.sortName,
                            title = ws.label ?: "",
                            desc = ws.description
                                ?: "", // TODO provide another string for nicer UI when the description when ws."",
                            mime = ws.type,
                            modifier = Modifier
                                .wrapContentSize(align = Alignment.Center)
                                .clickable { open(ws.getStateID()) },
                        )
                    }
                }

                if (workspaces.isEmpty() && cells.isEmpty() && loadingState == LoadingState.IDLE) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = stringResource(R.string.account_home_no_ws_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.account_home_no_ws_desc),
                            )
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(
            loadingState == LoadingState.PROCESSING || loadingState == LoadingState.STARTING,
            state,
            Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview(name = "HomeCard Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "HomeCard Dark Mode"
)
@Composable
private fun HomeCardPreview() {
    UseCellsTheme {
//        HomeCardItem(
//            "2_",
//            "alice",
//            "https://www.example.com",
//        )
    }
}
