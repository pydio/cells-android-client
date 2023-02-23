package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.core.composables.MainTitleText
import com.pydio.android.cells.ui.core.composables.getWsThumbVector
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.models.AccountHomeVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.LoadingState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private const val logTag = "AccountHome.kt"

@Composable
fun AccountHome(
    accountID: StateID,
    openDrawer: () -> Unit,
    openAccounts: () -> Unit,
    openSearch: () -> Unit,
    openWorkspace: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    accountHomeVM: AccountHomeVM = koinViewModel(),
) {

    LaunchedEffect(key1 = accountID) {
        Log.e(logTag, "... in AccountHome, launching effect")
        browseRemoteVM.watch(accountID, false)
    }
    val loadingState by browseRemoteVM.loadingState.observeAsState()
    accountHomeVM.setState(accountID)

    val sessionView by accountHomeVM.currSession.observeAsState()
    val workspaces by accountHomeVM.wss.observeAsState()
    val cells by accountHomeVM.cells.observeAsState()

    val title = sessionView?.serverLabel() ?: "${accountID.serverUrl} - Home"

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(accountID, true)
    }

    AccHomeScaffold(
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
private fun AccHomeScaffold(
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

        Column(Modifier.padding(padding)) {

            HomeHeader(
                username = stateID.username,
                address = stateID.serverUrl,
                openAccounts = openAccounts,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.bottom_sheet_item_h_padding))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .6f),
                thickness = 1.dp,
            )

            HomeList(
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
private fun HomeList(
    loadingState: LoadingState,
    workspaces: List<RWorkspace>,
    cells: List<RWorkspace>,
    open: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    modifier: Modifier,
) {
    val ctx = LocalContext.current

    // var refreshing by remember() { mutableStateOf(isLoading) }
    // Warning: pullRefresh API is:
    //   - experimental
    //   - only implemented in material 1, for the time being.
    Log.d(logTag, "Fist pass, is loading: $loadingState")

    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh()
    })

    Box(modifier.pullRefresh(state)) {
        LazyVerticalGrid(
            // TODO make this more generic for big screens also
            columns = GridCells.Adaptive(minSize = 128.dp),
            // columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(dimensionResource(R.dimen.grid_layout_card_margin)),
        ) {

            if (workspaces.isNotEmpty()) {
                item(span = {
                    // LazyGridItemSpanScope:
                    // maxLineSpan
                    GridItemSpan(maxLineSpan)
                }) {
                    MainTitleText(
                        text = "Workspaces",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                items(workspaces, key = { it.encodedState }) { ws ->
                    HomeCardItem(
                        ws.sortName ?: "",
                        ws.label ?: "",
                        ws.description ?: "",
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
                        ws.sortName ?: "",
                        ws.label ?: "",
                        ws.description ?: "",
                        modifier = Modifier.clickable { open(ws.getStateID()) },
                    )
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
    sortName: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        modifier = modifier
    ) {

        Surface(
            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.grid_ws_image_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
                .wrapContentSize(Alignment.Center)
        ) {
            Icon(
                imageVector = getWsThumbVector(sortName),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.list_thumb_size))
            )
        }
        Column(
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.grid_ws_content_h_padding),
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

    }
}

@Composable
private fun HomeItem(
    sortName: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Icon(
                imageVector = getWsThumbVector(sortName),
                contentDescription = "", // TODO
            )

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.list_thumb_margin)))

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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
        HomeCardItem(
            "2_",
            "alice",
            "https://www.example.com",
        )
    }
}
