package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.theme.CellsIcons

// private const val logTag = "GenericList "

@Composable
fun WithLoadingListBackground(
    loadingState: LoadingState,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    listContext: ListContext = ListContext.BROWSE,
    canRefresh: Boolean = true,
    showProgressAtStartup: Boolean = true,
    startingDesc: String = stringResource(R.string.loading_message),
    emptyRefreshableDesc: String = stringResource(R.string.empty_folder),
    emptyNoConnDesc: String = stringResource(R.string.empty_cache) + "\n" + stringResource(R.string.server_unreachable),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (isEmpty) {
            if (loadingState == LoadingState.STARTING) {
                Box(modifier = Modifier.fillMaxSize()) {
                    StartingIndicator(
                        desc = startingDesc,
                        showProgressAtStartup = showProgressAtStartup,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(.5f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    EmptyList(
                        listContext = listContext,
                        desc = if (canRefresh) {
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
        content()
    }
}

@Composable
fun StartingIndicator(
    modifier: Modifier = Modifier,
    showProgressAtStartup: Boolean = true,
    desc: String?
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (showProgressAtStartup) {
            CircularProgressIndicator()
        }
        desc?.let {
            Text(it)
        }
    }
}

@Composable
fun EmptyList(
    listContext: ListContext,
    modifier: Modifier = Modifier,
    desc: String?
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(dimensionResource(id = R.dimen.margin_large))
    ) {
        Icon(
            imageVector = getVectorFromListContext(listContext),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.grid_ws_image_size))
        )
        desc?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getVectorFromListContext(context: ListContext): ImageVector {

    return when (context) {
        ListContext.BOOKMARKS ->
            CellsIcons.Bookmark

        ListContext.OFFLINE ->
            CellsIcons.KeepOffline

        ListContext.BROWSE ->
            CellsIcons.EmptyFolder
    }
}

@Composable
fun BrowseUpItem(
    parentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(modifier) {
        Row(Modifier.padding(horizontal = 8.dp)) {
            Surface(
                Modifier
                // .size(40.dp)
                // .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                // .background(MaterialTheme.colorScheme.error)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_new_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        // .fillMaxSize()
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        .wrapContentSize(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.list_thumb_margin)))

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentSize(Alignment.CenterStart)
            ) {
                Text(
                    text = "..",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = parentDescription,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
