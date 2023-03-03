package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.ui.browse.models.AccountHomeVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.core.composables.GridThumb
import com.pydio.android.cells.ui.core.composables.MainTitleText
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID

private const val logTag = "AccountHome"

@Composable
fun AccountHome(
    accountID: StateID,
    openDrawer: () -> Unit,
    openAccounts: () -> Unit,
    openSearch: () -> Unit,
    openWorkspace: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    accountHomeVM: AccountHomeVM,
) {

    // FIXME this must not be done here anymore
    LaunchedEffect(key1 = accountID) {
        Log.e(logTag, "... in AccountHome, launching effect")
        browseRemoteVM.watch(accountID, false)
    }
    val loadingState by browseRemoteVM.loadingState.observeAsState()

    val sessionView by accountHomeVM.currSession.observeAsState()
    val workspaces by accountHomeVM.wss.observeAsState()
    val cells by accountHomeVM.cells.observeAsState()

    val title = sessionView?.serverLabel() ?: "${accountID.serverUrl} - Home"

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(accountID, true)
    }

    OfflineScaffold(
        stateID = accountID,
        title = title,
        workspaces = workspaces ?: listOf(),
        cells = cells ?: listOf(),
        loadingState = loadingState ?: LoadingState.STARTING,
        openDrawer = openDrawer,
        openAccounts = openAccounts,
        openWorkspace = openWorkspace,
        openSearch = openSearch,
        forceRefresh = forceRefresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineScaffold(
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
        modifier = Modifier.padding(
            horizontal = dimensionResource(id = R.dimen.margin)
        )
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:

        Log.e(logTag, "### About to create the list passed content padding")
        Log.e(logTag, "$padding")

        val listPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding(),
            start = dimensionResource(id = R.dimen.margin),
            end = dimensionResource(id = R.dimen.margin),
        )

        Column(Modifier.padding(padding)) {

            HomeHeader(
                username = stateID.username,
                address = stateID.serverUrl,
                openAccounts = openAccounts,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .3f),
                thickness = 0.3.dp,
            )

            AccountHomeList(
                loadingState = loadingState,
                workspaces = workspaces,
                cells = cells,
                open = openWorkspace,
                forceRefresh = forceRefresh,
                modifier = Modifier.fillMaxWidth(), // padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AccountHomeList(
    loadingState: LoadingState,
    workspaces: List<RWorkspace>,
    cells: List<RWorkspace>,
    open: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    modifier: Modifier,
) {

    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh()
    })

    Box(modifier.pullRefresh(state)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.margin)),
//            contentPadding = PaddingValues(dimensionResource(R.dimen.grid_layout_card_margin)),
        ) {

            if (workspaces.isNotEmpty()) {
                item(span = {
                    GridItemSpan(maxLineSpan)
                }) {
                    MainTitleText(
                        text = "Workspaces",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                items(workspaces, key = { it.encodedState }) { ws ->
                    HomeCardItem(
                        encodedState = ws.encodedState,
                        sortName = ws.sortName,
                        name = ws.label ?: "",
                        title = ws.label ?: "",
                        desc = ws.description ?: "",
                        mime = ws.type,
                        eTag = "",
                        hasThumb = false,
//                        ws.sortName ?: "",
//                        ws.label ?: "",
//                        ws.description ?: "",
                        modifier = Modifier
                            .wrapContentSize(align = Alignment.Center)
                            .clickable { open(ws.getStateID()) },
                    )
                }
            }

            if (cells.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(text = "Cells")
                }
                items(cells, key = { it.encodedState }) { ws ->
                    HomeCardItem(
                        encodedState = ws.encodedState,
                        sortName = ws.sortName,
                        name = ws.label ?: "",
                        title = ws.label ?: "",
                        desc = ws.description ?: "",
                        mime = ws.type,
                        eTag = "",
                        hasThumb = false,
//                        ws.sortName ?: "",
//                        ws.label ?: "",
//                        ws.description ?: "",
                        modifier = Modifier
                            .wrapContentSize(align = Alignment.Center)
                            .clickable { open(ws.getStateID()) },
                    )

//                    HomeCardItem(
//                        ws.sortName ?: "",
//                        ws.label ?: "",
//                        ws.description ?: "",
//                        modifier = Modifier.clickable { open(ws.getStateID()) },
//                    )
                }
            }

            if (workspaces.isEmpty() && cells.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(text = "Nothing to show (yet...)")
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

@Composable
private fun HomeCardItem(
    encodedState: String,
    sortName: String?,
    name: String,
    title: String,
    desc: String,
    mime: String,
    eTag: String?,
    hasThumb: Boolean,
    modifier: Modifier = Modifier
) {
    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 0.dp,
    )
    val descPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 0.dp,
        bottom = 8.dp,
    )

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        modifier = modifier
    ) {

        GridThumb(
            encodedState = encodedState,
            sortName = sortName,
            name = name,
            mime = mime,
            eTag = eTag,
            hasThumb = hasThumb,
            outerSize = dimensionResource(R.dimen.grid_ws_image_size),
            iconSize = dimensionResource(R.dimen.grid_icon_size),
            clipShape = RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(titlePadding)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(descPadding)
        )
    }
}

@Composable
private fun HomeHeader(
    username: String,
    address: String,
    openAccounts: () -> Unit,
    modifier: Modifier,
) {
    val buttonAlpha = getFloatResource(LocalContext.current, R.dimen.list_button_alpha)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Surface(
            modifier = Modifier
                .clickable { openAccounts() }
                .padding(
                    start = dimensionResource(id = R.dimen.margin_xsmall),
                    end = dimensionResource(id = R.dimen.margin_small)
                )
                .alpha(buttonAlpha)
        ) {
            Icon(
                imageVector = CellsIcons.SwitchAccount,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.list_button_size))
            )
        }
    }
}

@Preview(name = "HomeHeader Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "HomeHeader Dark Mode"
)
@Composable
private fun HomeHeaderPreview() {
    CellsTheme {
        HomeHeader(
            "alice",
            "https://www.example.com",
            { },
            Modifier.fillMaxWidth()
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
    CellsTheme {
//        HomeCardItem(
//            "2_",
//            "alice",
//            "https://www.example.com",
//        )
    }
}
