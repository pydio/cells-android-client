package com.pydio.android.cells.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.R
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.lists.EmptyList
import com.pydio.android.cells.ui.core.composables.lists.MultipleGridItem
import com.pydio.android.cells.ui.core.composables.lists.WithListTheme
import com.pydio.android.cells.ui.core.composables.lists.getAppearsInDesc
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.cells.transport.StateID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HitsList(
    connectionState: ConnectionState,
    query: String,
    listLayout: ListLayout,
    hits: List<MultipleItem>,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit,
    padding: PaddingValues,
) {

    WithSearchLoadingBackground(
        connectionState = connectionState,
        isEmpty = hits.isEmpty(),
        showProgressAtStartup = true,
        emptyRefreshableDesc = if (query.isEmpty()) {
            stringResource(R.string.search_hint, query)
        } else {
            stringResource(R.string.no_result_for_search, query)
        },
        emptyNoConnDesc = stringResource(R.string.server_unreachable) + ":\n" + stringResource(R.string.search_is_local_only),
        modifier = Modifier.fillMaxSize()
    ) {

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
                    columns = GridCells.Adaptive(minSize = dimensionResource(R.dimen.grid_col_min_width)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = listPadding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = hits,
                        key = { it.uuid }) { node ->
                        MultipleGridItem(
                            item = node,
                            more = { openMoreMenu(node.defaultStateID()) },
                            isSelectionMode = false,
                            isSelected = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { open(node.defaultStateID()) }
                                .animateItem(),
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(hits, key = { it.uuid }) { node ->
                        HitListItem(
                            item = node,
                            more = { openMoreMenu(node.defaultStateID()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { open(node.defaultStateID()) }
                                .animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WithSearchLoadingBackground(
    connectionState: ConnectionState,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    listContext: ListContext = ListContext.BROWSE,
    showProgressAtStartup: Boolean = false,
    emptyRefreshableDesc: String = stringResource(R.string.empty_folder),
    emptyNoConnDesc: String = stringResource(R.string.empty_cache) + ":\n" + stringResource(R.string.server_unreachable),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (isEmpty) {
            Box(modifier = Modifier.fillMaxSize()) {
                if ((connectionState.loading == LoadingState.STARTING && showProgressAtStartup)
                    || connectionState.loading == LoadingState.PROCESSING
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = modifier.padding(dimensionResource(R.dimen.margin_large))
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.spinner_size))
                                .padding(dimensionResource(R.dimen.margin_medium))
                                .alpha(.8f)
                        )
                        Text(stringResource(R.string.loading_message))
                    }
                } else {
                    EmptyList(
                        listContext = listContext,
                        desc = if (connectionState.serverConnection.isConnected()) {
                            emptyRefreshableDesc
                        } else {
                            emptyNoConnDesc
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(.5f)
                    )
                }
            }
        }
        WithListTheme {
            content()
        }
    }
}


@Composable
private fun HitListItem(
    item: MultipleItem,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                top = dimensionResource(R.dimen.list_item_inner_padding),
                bottom = dimensionResource(R.dimen.list_item_inner_padding),
                start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
                end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
            )
    ) {

        Thumbnail(
            item.defaultStateID(), item.sortName, item.name, item.mime, item.eTag, -1, item.hasThumb
        )

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
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = getNodeDesc(item.remoteModTs, item.size, null),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = getAppearsInDesc(item),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        IconButton(onClick = { more() }) {
            Icon(
                painter = painterResource(R.drawable.aa_300_more_vert_40px),
                contentDescription = stringResource(R.string.open_more_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
            )
        }
    }
}
