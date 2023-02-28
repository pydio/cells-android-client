package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.getNodeDesc
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.models.BookmarksVM
import com.pydio.android.cells.ui.browse.models.MoreMenuType
import com.pydio.android.cells.ui.browse.models.MoreMenuVM
import com.pydio.android.cells.ui.browse.models.NodeMoreMenuData
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val logTag = "Bookmarks.kt"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Bookmarks(
    accountID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    bookmarksVM: BookmarksVM,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingState by browseRemoteVM.loadingState.observeAsState()

    val bookmarks = bookmarksVM.bookmarks.observeAsState()

    val forceRefresh: () -> Unit = {
        bookmarksVM.forceRefresh(accountID)
    }

    val localOpen: (StateID) -> Unit = { stateID ->
        scope.launch {
            bookmarksVM.getNode(stateID)?.let {
                if (it.isFolder()) {
                    open(stateID)
//                } else if (it.isPreViewable()) {
                    // TODO (since v2) Open carousel for bookmark nodes
                } else {
                    bookmarksVM.viewFile(context, stateID)
                }
            }
        }
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

    val destinationPicker = rememberLauncherForActivityResult(
        // TODO we have the mime of the file to download to device
        //    but this is no trivial implementation: the contract must then be both
        //    dynamic AND remembered.
        contract = ActivityResultContracts.CreateDocument(),
        onResult = { uri ->
            if (moreMenuData.value != Transport.UNDEFINED_STATE_ID) {
                uri?.let {
                    bookmarksVM.download(moreMenuData.value, uri)
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
                    bookmarksVM.getNode(stateID)?.let {
                        if (it.isFolder()) {
                            open(stateID)
                        } else {
                            open(stateID.parent())
                        }
                    }
                }
            }
            is NodeAction.DownloadToDevice -> {
                destinationPicker.launch(stateID.fileName)
                // Done is called by the destination picker callback
            }
            is NodeAction.ToggleBookmark -> {
                bookmarksVM.removeBookmark(stateID)
                moreMenuDone()
            }
            else -> {
                Log.e(logTag, "Unknown action $action for $stateID")
                moreMenuDone()
            }
        }
    }

    BookmarkScaffold(
        loadingState = loadingState ?: LoadingState.STARTING,
        title = stringResource(id = R.string.action_open_bookmarks),
        bookmarks = bookmarks.value ?: listOf(),
        openSearch = openSearch,
        openDrawer = openDrawer,
        forceRefresh = forceRefresh,
        open = localOpen,
        launch = launch,
        moreMenu = Triple(moreMenuState, moreMenuData.value, openMoreMenu),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkScaffold(
    loadingState: LoadingState,
    title: String,
    bookmarks: List<RTreeNode>,
    openSearch: () -> Unit,
    openDrawer: () -> Unit,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    launch: (NodeAction, StateID) -> Unit,
    moreMenu: Triple<ModalBottomSheetState, StateID, (StateID) -> Unit>,
) {

    val moreMenuVM: MoreMenuVM = koinViewModel()
    val tint = MaterialTheme.colorScheme.onSurface
    val bgColor = MaterialTheme.colorScheme.surface

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
                    type = MoreMenuType.BOOKMARK,
                    toOpenStateID = moreMenu.second,
                    launch = { launch(it, moreMenu.second) },
                    moreMenuVM = moreMenuVM,
                    tint = tint,
                    bgColor = bgColor,
                )
            },
            modifier = Modifier,
            sheetState = moreMenu.first,
            sheetBackgroundColor = bgColor,
        ) {

            BookmarkList(
                loadingState = loadingState,
                bookmarks = bookmarks,
                forceRefresh = forceRefresh,
                openMoreMenu = moreMenu.third,
                open = open,
                padding = padding,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BookmarkList(
    loadingState: LoadingState,
    bookmarks: List<RTreeNode>,
    forceRefresh: () -> Unit,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
) {

    val context = LocalContext.current

    val state = rememberPullRefreshState(
        loadingState == LoadingState.PROCESSING,
        onRefresh = {
            Log.i(logTag, "Force refresh launched")
            forceRefresh()
        },
    )

    Box(modifier.pullRefresh(state)) {
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(bookmarks) { node ->
                NodeItem(
                    item = node,
                    title = getNodeTitle(name = node.name, mime = node.mime),
                    desc = getNodeDesc(
                        context,
                        node.remoteModificationTS,
                        node.size,
                        node.localModificationStatus
                    ),
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

