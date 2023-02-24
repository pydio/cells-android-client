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
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.getNodeDesc
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.LoadingState
import com.pydio.cells.transport.StateID

private const val logTag = "Bookmarks.kt"

@Composable
fun Bookmarks(
    accountID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    bookmarksVM: BookmarksVM,
) {
    val loadingState by browseRemoteVM.loadingState.observeAsState()

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(accountID, true)
    }

    val bookmarks = bookmarksVM.bookmarks.observeAsState()

    val openInWorkspaces: (StateID) -> Unit = {
        // TODO
        // put the checks to see what we open here
    }

    OfflineScaffold(
        title = stringResource(id = R.string.action_open_bookmarks),
        loadingState = loadingState ?: LoadingState.STARTING,
        stateID = accountID,
        bookmarks = bookmarks.value ?: listOf(),
        openDrawer = openDrawer,
        openSearch = openSearch,
        forceRefresh = forceRefresh,
        openInWorkspaces = openInWorkspaces,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineScaffold(
    title: String,
    loadingState: LoadingState,
    stateID: StateID,
    bookmarks: List<RTreeNode>,
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
            bookmarks = bookmarks,
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
    bookmarks: List<RTreeNode>,
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
            items(bookmarks) { node ->
                NodeItem(
                    item = node,
                    mime = node.mime,
                    sortName = node.sortName,
                    title = getNodeTitle(name = node.name, mime = node.mime),
                    desc = getNodeDesc(
                        context,
                        node.remoteModificationTS,
                        node.size,
                        node.localModificationStatus
                    ),
                    isBookmarked = node.isBookmarked(),
                    isOfflineRoot = node.isOfflineRoot(),
                    isShared = node.isShared(),
                    more = {
                        openMoreMenu(node.getStateID())
                    },
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

//
//
//@Composable
//private fun BookmarkItem(
//    sortName: String,
//    title: String,
//    desc: String,
//    modifier: Modifier = Modifier
//) {
//
//    Card(
//        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
//        ),
//        modifier = modifier
//    ) {
//
//        Surface(
//            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
//            modifier = Modifier
//                .size(dimensionResource(id = R.dimen.grid_ws_image_size))
//                .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
//                .wrapContentSize(Alignment.Center)
//        ) {
//            Icon(
//                imageVector = getWsThumbVector(sortName),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(dimensionResource(R.dimen.list_thumb_size))
//            )
//        }
//        Column(
//            modifier = Modifier.padding(
//                horizontal = dimensionResource(R.dimen.grid_ws_content_h_padding),
//            )
//        ) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleMedium,
//            )
//            Text(
//                text = desc,
//                style = MaterialTheme.typography.bodyMedium,
//            )
//        }
//
//    }
//}
//
//@Preview(name = "Bookmark Light Mode")
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "Bookmark Dark Mode"
//)
//@Composable
//private fun BookmarkItemPreview() {
//    CellsTheme {
//        BookmarkItem(
//            "2_",
//            "alice",
//            "https://www.example.com",
//        )
//    }
//}
