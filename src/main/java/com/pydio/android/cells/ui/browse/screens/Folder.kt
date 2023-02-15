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
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.ModalBottomSheetValue
import com.pydio.android.cells.ui.box.beta.bottomsheet.modal.rememberModalBottomSheetState
import com.pydio.android.cells.ui.browse.TreeNodeVM
import com.pydio.android.cells.ui.browse.composables.NodeItem
import com.pydio.android.cells.ui.browse.composables.TreeNodeBottomSheet
import com.pydio.android.cells.ui.browse.composables.getNodeDesc
import com.pydio.android.cells.ui.browse.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.BrowseUpItem
import com.pydio.android.cells.ui.core.composables.DefaultTopBar
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.theme.CellsVectorIcons
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val logTag = "Folder.kt"

@Composable
fun Folder(
    stateID: StateID,
    openDrawer: () -> Unit,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openSearch: () -> Unit,
    browseRemoteVM: BrowseRemoteVM,
    browseLocalVM: BrowseLocalFoldersVM = koinViewModel(),
) {

    Log.e(logTag, "... Composing Folder for $stateID")

    LaunchedEffect(key1 = stateID) {
        Log.e(logTag, "... in Folder, launching effect")
        browseRemoteVM.watch(stateID)
    }

    browseLocalVM.setState(stateID)
    val isLoading by browseRemoteVM.isLoading.observeAsState()
    val children by browseLocalVM.childNodes.observeAsState()

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(stateID)
    }

    FolderWithMoreMenu(
        isLoading = isLoading ?: true,
        stateID = stateID,
        children = children ?: listOf(),
        openDrawer = openDrawer,
        openParent = openParent,
        open = open,
        openSearch = openSearch,
        forceRefresh = forceRefresh,
    )
}

/** Add the more menu **/
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FolderWithMoreMenu(
    isLoading: Boolean,
    stateID: StateID,
    children: List<RTreeNode>,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    openDrawer: () -> Unit,
    openSearch: () -> Unit,
    treeNodeVM: TreeNodeVM = koinViewModel(),
) {

    val scope = rememberCoroutineScope()

    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val childState: MutableState<RTreeNode?> = remember {
        mutableStateOf(null)
    }

    val openMoreMenu: (StateID) -> Unit = { childID ->
        scope.launch {
            val currNode = treeNodeVM.getTreeNode(childID) ?: run {
                Log.e(logTag, "No node found for $childID, aborting")
                return@launch
            }
            childState.value = currNode
            state.expand()
        }
    }

    val doAction: (String, StateID) -> Unit = { action, stateID ->
        scope.launch {
            when (action) {
                // TODO
            }
            childState.value = null
            state.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetContent = { TreeNodeBottomSheet(treeNodeVM, childState.value, doAction) },
        modifier = Modifier,
        state
    ) {
        FolderScaffold(
            isLoading = isLoading,
            label = stateID.fileName ?: stateID.workspace,// FIXME
            stateID = stateID,
            children = children,
            openDrawer = openDrawer,
            openSearch = openSearch,
            openParent = openParent,
            openMoreMenu = openMoreMenu,
            open = open,
            forceRefresh = forceRefresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderScaffold(
    isLoading: Boolean,
    stateID: StateID,
    label: String,
    children: List<RTreeNode>,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openDrawer: () -> Unit,
    openMoreMenu: (StateID) -> Unit,
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
            if (Str.notEmpty(stateID.workspace)) {
                FloatingActionButton(
                    onClick = { /*TODO*/ }
                ) { Icon(Icons.Filled.Add, /* TODO */ contentDescription = "") }
            }
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:
        Column {
            FolderList(
                refreshing = isLoading,
                stateID = stateID,
                children = children,
                openParent = openParent,
                open = open,
                openMoreMenu = openMoreMenu,
                forceRefresh = forceRefresh,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FolderList(
    refreshing: Boolean,
    stateID: StateID,
    children: List<RTreeNode>,
    openParent: (StateID) -> Unit,
    open: (StateID) -> Unit,
    openMoreMenu: (StateID) -> Unit,
    forceRefresh: () -> Unit,
    modifier: Modifier,
) {
    // var refreshing by remember() { mutableStateOf(isLoading) }
    // Warning: pullRefresh API is:
    //   - experimental
    //   - only implemented in material 1, for the time being.
    val state = rememberPullRefreshState(refreshing, onRefresh = {
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
                    mime = node.mime,
                    sortName = node.sortName,
                    title = getNodeTitle(name = node.name, mime = node.mime),
                    desc = getNodeDesc(context, node.remoteModificationTS, node.size),
                    more = { openMoreMenu(node.getStateID()) },
                    modifier = Modifier.clickable { open(node.getStateID()) },
                )
            }
        }
        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
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
                    CellsVectorIcons.Menu,
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
                    CellsVectorIcons.Search,
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
