package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsListTypography

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
            Box(modifier = Modifier.fillMaxSize()) {
                if (loadingState == LoadingState.STARTING) {
                    StartingBackground(
                        desc = startingDesc,
                        showProgressAtStartup = showProgressAtStartup,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(.5f)
                    )

                } else {

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
        WithListTheme {
            content()
        }
    }
}

@Composable
fun WithListTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = CellsListTypography
    ) {
        content()
    }

}

@Composable
fun StartingBackground(
    modifier: Modifier = Modifier,
    showProgressAtStartup: Boolean = true,
    desc: String?
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(dimensionResource(R.dimen.margin_large))
    ) {
        if (showProgressAtStartup) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.spinner_size))
                    .padding(dimensionResource(R.dimen.margin_medium))
                    .alpha(.8f)
            )
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
        modifier = modifier.padding(dimensionResource(R.dimen.margin_large))
    ) {
        Icon(
            imageVector = getVectorFromListContext(listContext),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(R.dimen.list_empty_icon_size))
                .padding(dimensionResource(R.dimen.margin_medium))
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
        ListContext.ACCOUNTS ->
            CellsIcons.AccountCircle

        ListContext.BOOKMARKS ->
            CellsIcons.Bookmark

        ListContext.OFFLINE ->
            CellsIcons.KeepOffline

        ListContext.BROWSE ->
            CellsIcons.EmptyFolder

        ListContext.TRANSFERS ->
            CellsIcons.Processing

        ListContext.SEARCH ->
            CellsIcons.Search

        ListContext.SYSTEM ->
            CellsIcons.Jobs
    }
}
