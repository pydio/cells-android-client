package com.pydio.android.cells.ui.share.screens

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.ui.browse.composables.CreateFolder
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.models.NodeActionsVM
import com.pydio.android.cells.ui.core.actionRoute
import com.pydio.android.cells.ui.core.actionRouteTemplate
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.M3BrowseUpListItem
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private const val LOG_TAG = "SelectFolder.kt"

private const val SELECT_FOLDER_PAGE = "select-folder"

@Composable
fun SelectFolderScreen(
    targetAction: String,
    stateID: StateID,
    subjects: Set<StateID> = setOf(),
    browseRemoteVM: BrowseRemoteVM,
    shareVM: ShareVM,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    doAction: (String, StateID) -> Unit,
) {

    val navController = rememberNavController()

    val connectionState = browseRemoteVM.connectionState.collectAsState()
    val children = shareVM.children.collectAsState()

    val forbiddenIDs: MutableState<Set<StateID>> = remember { mutableStateOf(setOf()) }

    LaunchedEffect(key1 = subjects.toString()) {
        //Log.d(LOG_TAG, "Launched effect for $subjects")
        // Reinitialise values
        try {
            val founds = mutableSetOf<StateID>()

            for (subject in subjects) {
                if (subject != StateID.NONE) {
                    shareVM.getTreeNode(subject)?.let { currNode ->
                        if (currNode.isFolder()) {
                            founds.add(currNode.getStateID())
                        }
                    }
                }
            }
            forbiddenIDs.value = founds
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Unexpected error while computing forbidden path for $stateID")
            e.printStackTrace()
        }
    }

    val isForbiddenTarget: (TreeNodeItem) -> Boolean = {
        if (!it.isFolder || it.isRecycle) {
            true
        } else {
            forbiddenIDs.value.contains(it.stateID)
        }
    }

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(stateID, true)
    }

    val interceptAction: (String, StateID) -> Unit = { action, currID ->
        Log.e(LOG_TAG, "Intercepting $action for $currID")
        if (AppNames.ACTION_CREATE_FOLDER == action) {
            navController.navigate(actionRoute(NodeAction.CreateFolder, currID))
        } else {
            doAction(action, currID)
        }
    }

    val closeDialog: (Boolean) -> Unit = { done ->
        navController.popBackStack(SELECT_FOLDER_PAGE, false)
        if (done) {
            browseRemoteVM.watch(stateID, true)
        }
    }

    NavHost(navController, SELECT_FOLDER_PAGE) {

        composable(SELECT_FOLDER_PAGE) {  // Fills the area provided to the NavHost
            LaunchedEffect(key1 = stateID) {
                Log.i(LOG_TAG, "## First Composition for: $SELECT_FOLDER_PAGE/$stateID")
            }
            SelectFolderScaffold(
                connectionState = connectionState.value,
                action = targetAction,
                stateID = stateID,
                children = children.value,
                forceRefresh = forceRefresh,
                open = open,
                canPost = canPost,
                isForbiddenTarget = isForbiddenTarget,
                doAction = interceptAction,
            )
        }

        dialog(actionRouteTemplate(NodeAction.CreateFolder)) { entry ->
            val currID = lazyStateID(entry)
            if (currID == StateID.NONE) {
                Log.w(LOG_TAG, "Cannot create folder with no ID")
                navController.popBackStack(SELECT_FOLDER_PAGE, false)
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

@Composable
fun SelectFolderScaffold(
    connectionState: ConnectionState,
    action: String,
    stateID: StateID,
    children: List<TreeNodeItem>,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    isForbiddenTarget: (TreeNodeItem) -> Boolean,
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
            if (!stateID.slug.isNullOrEmpty()) {
                FloatingActionButton(
                    onClick = {
                        doAction(AppNames.ACTION_CREATE_FOLDER, stateID)
                    }
                ) { Icon(Icons.Filled.Add, contentDescription = "Create folder") }
            }
        },
    ) { padding ->
        SelectFolderList(
            action = action,
            stateID = stateID,
            children = children,
            connectionState = connectionState,
            forceRefresh = forceRefresh,
            open = open,
            isForbiddenTarget = isForbiddenTarget,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SelectFolderList(
    action: String,
    stateID: StateID,
    children: List<TreeNodeItem>,
    connectionState: ConnectionState,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    isForbiddenTarget: (TreeNodeItem) -> Boolean,
    modifier: Modifier,
) {
    val alpha = getFloatResource(LocalContext.current, R.dimen.disabled_list_item_alpha)
    val paddingValues = PaddingValues(
        top = dimensionResource(R.dimen.list_item_inner_padding),
        bottom = dimensionResource(R.dimen.list_item_inner_padding),
        start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
        end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
    )
    val state = rememberPullRefreshState(
        connectionState.loading.isRunning(),
        onRefresh = { forceRefresh() },
    )
    Box(modifier.pullRefresh(state)) {
        LazyColumn(Modifier.fillMaxWidth()) {
            // For the time being we only support intra workspace copy / move
            // We so reduce the "up" row visibility at the WS level when in such situation
            if (!stateID.fileName.isNullOrEmpty() || action == AppNames.ACTION_UPLOAD) {
                item {
                    val parentDescription = when {
                        stateID.path.isNullOrEmpty() -> stringResource(id = R.string.switch_account)
                        stateID.fileName.isNullOrEmpty() -> stringResource(id = R.string.switch_workspace)
                        else -> stringResource(R.string.parent_folder)
                    }
                    val targetID = if (stateID.slug.isNullOrEmpty()) {
                        StateID.NONE
                    } else {
                        stateID.parent()
                    }
                    M3BrowseUpListItem(
                        parentDesc = parentDescription,
                        modifier = Modifier.clickable { open(targetID) }
                    )
                }
            }
            items(children) { oneChild ->
                val currModifier = if (isForbiddenTarget(oneChild)) {
                    Modifier.alpha(alpha)
                } else {
                    Modifier.clickable { open(oneChild.stateID) }
                }
                SelectFolderItem(oneChild, currModifier.padding(paddingValues))
            }
        }
        PullRefreshIndicator(
            connectionState.loading.isRunning(),
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    val title = when (action) {
        AppNames.ACTION_UPLOAD -> stringResource(R.string.choose_target_for_share_title)
        AppNames.ACTION_COPY -> stringResource(R.string.choose_target_for_copy_title)
        AppNames.ACTION_MOVE -> stringResource(R.string.choose_target_for_move_title)
        else -> stringResource(R.string.choose_target_subtitle)
    }
    // TODO configure ellipsize from start (or middle?) rather than from the end
    val subTitle = stateID.path ?: "${stateID.username}@${stateID.serverHost}"

    Surface(color = bgColor, modifier = modifier.fillMaxWidth()) {
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
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subTitle,
                    color = textColor,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    bgColor,
                    textColor,
                    bgColor,
                    textColor.copy(alpha = .2f)
                ),
                onClick = { onSelect(action, stateID) },
                enabled = canPost(stateID)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Select this target")
            }
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(bgColor, textColor),
                onClick = { onSelect(AppNames.ACTION_CANCEL, stateID) }
            ) {
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
