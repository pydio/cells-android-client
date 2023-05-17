package com.pydio.android.cells.ui.share.screens

import android.content.res.Configuration
import android.util.Log
import android.util.TypedValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.composables.CreateFolder
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpListItem
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel

private const val logTag = "SelectFolder"

private const val FOLDER_MAIN_CONTENT = "folder-main-content"
private const val STATE_ID_SUFFIX = "/{state-id}"

private fun routeTemplate(action: NodeAction): String {
    return "${action.id}$STATE_ID_SUFFIX"
}

private fun route(action: NodeAction, stateID: StateID): String {
    return "${action.id}/${stateID.id}"
}

@Composable
fun SelectFolderScreen(
    stateID: StateID,
    browseRemoteVM: BrowseRemoteVM,
    shareVM: ShareVM,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    startUpload: (ShareVM, StateID) -> Unit,
    doAction: (String, StateID) -> Unit,
) {

    val navController = rememberNavController()

    val loadingStatus = browseRemoteVM.loadingState.collectAsState()
    val children = shareVM.children.collectAsState()

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(stateID, true)
    }

    val interceptAction: (String, StateID) -> Unit = { action, currID ->
        Log.e(logTag, "Intercepting $action for $currID")
        if (AppNames.ACTION_UPLOAD == action) {
            startUpload(shareVM, currID)
        } else if (AppNames.ACTION_CREATE_FOLDER == action) {
            navController.navigate(route(NodeAction.CreateFolder, currID))
        } else {
            doAction(action, currID)
        }
    }

    val closeDialog: (Boolean) -> Unit = { done ->
        navController.popBackStack(FOLDER_MAIN_CONTENT, false)
        if (done) {
            browseRemoteVM.watch(stateID, true)
        }
    }

    NavHost(navController, FOLDER_MAIN_CONTENT) {

        composable(FOLDER_MAIN_CONTENT) {  // Fills the area provided to the NavHost
            Log.d(logTag, "... Navigating to main content for $stateID")
            SelectFolderScaffold(
                loadingStatus = loadingStatus.value,
                action = AppNames.ACTION_UPLOAD,
                stateID = stateID,
                children = children.value,
                forceRefresh = forceRefresh,
                open = open,
                canPost = canPost,
                doAction = interceptAction,
            )
        }

        dialog(routeTemplate(NodeAction.CreateFolder)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(logTag, "Cannot create folder with no ID")
                navController.popBackStack(FOLDER_MAIN_CONTENT, false)
            } else {
                val nodeActionsVM: NodeActionsVM = koinViewModel()
                CreateFolder(
                    nodeActionsVM,
                    stateID = currID,
                    dismiss = { closeDialog(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFolderScaffold(
    loadingStatus: LoadingState,
    action: String,
    stateID: StateID,
    children: List<TreeNodeItem>,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    doAction: (String, StateID) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                action,
                stateID = stateID,
                canPost = canPost,
                onSelect = doAction,
            )
        },
        floatingActionButton = {
            if (Str.notEmpty(stateID.slug)) {
                FloatingActionButton(
                    onClick = {
                        doAction(AppNames.ACTION_CREATE_FOLDER, stateID)
                    }
                ) { Icon(Icons.Filled.Add, contentDescription = "Create folder") }
            }
        },
    ) { padding ->
        FolderList(
            action = action,
            stateID = stateID,
            children = children,
            loadingStatus = loadingStatus,
            forceRefresh = forceRefresh,
            open = open,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FolderList(
    action: String,
    stateID: StateID,
    children: List<TreeNodeItem>,
    loadingStatus: LoadingState,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    modifier: Modifier,
) {
    val state = rememberPullRefreshState(
        loadingStatus == LoadingState.PROCESSING,
        onRefresh = { forceRefresh() },
    )

    Box(modifier.pullRefresh(state)) {
        LazyColumn(Modifier.fillMaxWidth()) {
            // For the time being we only support intra workspace copy / move
            // We so reduce the "up" row visibility at the WS level when in such situation
            if (Str.notEmpty(stateID.fileName) || action == AppNames.ACTION_UPLOAD) {

                item {
                    val parentDescription = when {
                        Str.empty(stateID.path) -> stringResource(id = R.string.switch_account)
                        Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
                        else -> stringResource(R.string.parent_folder)
                    }
                    val targetID = if (Str.empty(stateID.slug)) {
                        StateID.NONE
                    } else {
                        stateID.parent()
                    }
                    M3BrowseUpListItem(
                        parentDescription = parentDescription,
                        modifier = Modifier.clickable { open(targetID) }
                    )
                }
            }
            items(children) { oneChild ->
                val currModifier = if (oneChild.isFolder) {
                    Modifier.clickable { open(oneChild.stateID) }
                } else {
                    Modifier
                }

                SelectFolderItem(
                    oneChild,
                    modifier = currModifier,
                )
            }
        }
        PullRefreshIndicator(
            loadingStatus == LoadingState.PROCESSING,
            state,
            Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun SelectFolderItem(
    item: TreeNodeItem,
    modifier: Modifier = Modifier
) {

    val alpha = if (!item.isFolder) {
        val outValue = TypedValue()
        LocalContext.current.resources.getValue(R.dimen.disabled_list_item_alpha, outValue, true)
        outValue.float
    } else {
        1f
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .alpha(alpha)
            .padding(
                top = dimensionResource(R.dimen.list_item_inner_padding),
                bottom = dimensionResource(R.dimen.list_item_inner_padding),
                start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
                end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
            )

    ) {

        Thumbnail(
            item.stateID,
            item.sortName,
            item.name,
            item.mime,
            item.eTag,
            item.metaHash,
            item.hasThumb,
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
                text = getNodeTitle(item.name, item.mime),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            val desc = if (item.isWsRoot) item.desc ?: "-" else {
                getNodeDesc(
                    remoteModificationTS = item.remoteModTs,
                    size = item.size,
                    localModificationStatus = item.localModStatus
                )
            }
            Text(
                text = desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TopBar(
    action: String,
    stateID: StateID,
    canPost: (StateID) -> Boolean,
    onSelect: (String, StateID) -> Unit,
    modifier: Modifier = Modifier
) {

    val title = when (action) {
        AppNames.ACTION_UPLOAD -> stringResource(R.string.choose_target_for_share_title)
        AppNames.ACTION_COPY -> stringResource(R.string.choose_target_for_copy_title)
        AppNames.ACTION_MOVE -> stringResource(R.string.choose_target_for_move_title)
        else -> stringResource(R.string.choose_target_subtitle)
    }
    // TODO configure ellipsize from start (or middle?) rather than from the end
    val subTitle = stateID.path ?: "${stateID.username}@${stateID.serverHost}"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.margin_small),
                    vertical = dimensionResource(R.dimen.margin_small),
                )
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            IconButton(
                onClick = { onSelect(action, stateID) },
                enabled = canPost(stateID)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Select this target")
            }
            IconButton(onClick = { onSelect(AppNames.ACTION_CANCEL, stateID) }) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel activity")
            }
        }
    }
}

@Preview(name = "TableHeader Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "TableHeader Dark"
)
@Composable
private fun TableHeaderPreview() {
    val state = StateID("lucy", "http://example.com", "/all-files/dummy")
    UseCellsTheme {
        TopBar(
            "",
            state,
            { true },
            { _, _ -> },
            Modifier.fillMaxWidth()
        )
    }
}

//@Preview(name = "Light Mode")
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "Dark Mode"
//)
//@Composable
//private fun FolderItemPreview() {
//   UseCellsTheme {
//        SelectFolderItem(
//            true,
//            SdkNames.NODE_MIME_FOLDER,
//            "2_WS on encrypted",
//            "WS on encrypted",
//            "29 October 2020 • 81 MB",
//            Modifier
//        )
//    }
//}
