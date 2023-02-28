package com.pydio.android.cells.ui.browse.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.aaLegacy.box.beta.bottomsheet.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.WrapWithActions
import com.pydio.android.cells.ui.browse.composables.getNodeDesc
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.browse.models.MoreMenuType
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.BrowseUpItem
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private const val logTag = "Folder.kt"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Folder(
    stateID: StateID,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    open: (StateID) -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    folderVM: FolderVM,
) {
    LaunchedEffect(key1 = stateID) {
        Log.e(logTag, "... in Folder, launching effect")
        browseRemoteVM.watch(stateID, false)
    }

    val loadingState by browseRemoteVM.loadingState.observeAsState()
    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(stateID, true)
    }

    val children by folderVM.childNodes.observeAsState()

    val treeNode by folderVM.treeNode.collectAsState()
    val workspace by folderVM.workspace.collectAsState()

    val binLabel = stringResource(R.string.recycle_bin_label)

    val label by remember(key1 = treeNode, key2 = workspace) {
        derivedStateOf {
            var tmpLabel = stateID.fileName ?: workspace?.label ?: stateID.workspace
            if (treeNode?.isRecycle() == true) {
                tmpLabel = binLabel
            }
            tmpLabel
        }
    }
//    val showFAB = Str.notEmpty(stateID.workspace) && !folderVM.isInRecycle()
//    Log.e(logTag, "After computing show fab: $showFAB")
    val showFAB by remember(key1 = treeNode) {
        derivedStateOf {
            val inRecycle = treeNode?.isRecycle() == true || treeNode?.isRecycle() == true
            // This is never the case -> useless check
            // val isNotAccountHome = Str.notEmpty(stateID.workspace)
            Log.e(logTag, "Derived state of show fab: $inRecycle")
            !inRecycle
        }
    }

    // We handle the state of the more menu here, not optimal...
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val childState: MutableState<Pair<MoreMenuType, StateID?>> = remember {
        mutableStateOf(Pair(MoreMenuType.NONE, null))
    }
    val openMoreMenu: (MoreMenuType, StateID) -> Unit = { type, childID ->
        scope.launch {
            childState.value = Pair(type, childID)
            state.expand()
        }
    }

    val actionDone: (Boolean) -> Unit = {
        scope.launch {
            childState.value = Pair(MoreMenuType.NONE, null)
            if (it) { // Also reset backoff ticker
                browseRemoteVM.watch(stateID, true) // TODO is it a force refresh here ?
            }
            state.hide()
        }
    }

    WrapWithActions(
        loadingState = loadingState ?: LoadingState.STARTING,
        actionDone = actionDone,
        type = childState.value.first,
        toOpenStateID = childState.value.second,
        sheetState = state,
    ) {
        FolderPage(
            loadingState = loadingState ?: LoadingState.STARTING,
            label = label,
            stateID = stateID,
            children = children ?: listOf(),
            showFAB = showFAB,
            openDrawer = openDrawer,
            openSearch = openSearch,
            openParent = { open(stateID.parent()) },
            openMoreMenu = openMoreMenu,
            open = open,
            forceRefresh = forceRefresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPage(
    loadingState: LoadingState,
    stateID: StateID,
    label: String,
    children: List<RTreeNode>,
    showFAB: Boolean,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openDrawer: () -> Unit,
    openMoreMenu: (MoreMenuType, StateID) -> Unit,
    openSearch: () -> Unit,
    forceRefresh: () -> Unit,
) {

    Scaffold(
        topBar = {
            DefaultTopBar(
                title = label,
                openDrawer = openDrawer,
                openSearch = openSearch,
            )
        },
        floatingActionButton = {
            if (showFAB) {
                FloatingActionButton(onClick = { openMoreMenu(MoreMenuType.CREATE, stateID) }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.fab_transformation_sheet_behavior)
                    )
                }
            }
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:
        Column {
            FolderList(
                loadingState = loadingState,
                stateID = stateID,
                children = children,
                openParent = openParent,
                open = open,
                openMoreMenu = { openMoreMenu(MoreMenuType.MORE, it) },
                forceRefresh = forceRefresh,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FolderList(
    loadingState: LoadingState,
    stateID: StateID,
    children: List<RTreeNode>,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    modifier: Modifier,
) {
    // WARNING: pullRefresh API is:
    //   - experimental
    //   - only implemented in material "1" for the time being.
    val state = rememberPullRefreshState(loadingState == LoadingState.PROCESSING, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh()
    })

    val context = LocalContext.current

    Box(modifier.pullRefresh(state)) {
        LazyColumn(Modifier.fillMaxWidth()) {
            if (Str.notEmpty(stateID.path)) {
                item {
                    val parentDescription = when {
                        Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
                        else -> stringResource(R.string.parent_folder)
                    }
                    BrowseUpItem(
                        parentDescription,
                        Modifier
                            .fillMaxWidth()
                            .clickable { openParent(stateID) }
                    )
                }
            }
            items(children) { node ->
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

@Composable
fun FolderTopBar(
    title: String,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.topbar_horizontal_padding),
                    vertical = dimensionResource(R.dimen.topbar_vertical_padding),
                )
        ) {
            IconButton(
                onClick = { openDrawer() },
                enabled = true
            ) {
                Icon(
                    CellsIcons.Menu,
                    contentDescription = stringResource(id = R.string.open_drawer)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            IconButton(onClick = { openSearch() }) {
                Icon(
                    CellsIcons.Search,
                    contentDescription = stringResource(id = R.string.action_search)
                )
            }
        }
    }
}

@Preview(name = "Folder Header Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "FolderTopBar Dark Mode"
)
@Composable
private fun FolderTopBarPreview() {
    CellsTheme {
        FolderTopBar(
            "alice",
            { },
            { },
            Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun TopBarPreview() {
    CellsTheme {
        FolderTopBar(
            "Pydio Cells server",
            { },
            { },
            Modifier.fillMaxWidth()
        )
    }
}
