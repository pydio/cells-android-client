package com.pydio.android.cells.ui.share.screens

import android.content.res.Configuration
import android.util.TypedValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.BrowseUpItem
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.isFolder
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.LoadingState
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel

private const val logTag = "SelectFolder.kt"

@Composable
fun SelectFolderScreen(
    stateID: StateID,
    browseRemoteVM: BrowseRemoteVM,
    open: (StateID) -> Unit,
    doAction: (String, StateID) -> Unit,
    browseLocalVM: BrowseLocalFoldersVM = koinViewModel(),
) {

    browseLocalVM.setState(stateID)

    val loadingStatus = browseRemoteVM.loadingState.observeAsState(LoadingState.STARTING)
    val childNodes by browseLocalVM.childNodes.observeAsState()

    val forceRefresh: () -> Unit = {
        browseRemoteVM.watch(stateID, true)
    }

    val canPost: (StateID) -> Boolean = {
        // FIXME also check permissions on remote server
        stateID.workspace.isNotBlank()
    }

    SelectFolderScaffold(
        loadingStatus = loadingStatus.value,
        action = AppNames.ACTION_UPLOAD,
        stateID = stateID,
        children = childNodes ?: listOf(),
        forceRefresh = forceRefresh,
        open = open,
        canPost = canPost,
        doAction = doAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFolderScaffold(
    loadingStatus: LoadingState,
    action: String,
    stateID: StateID,
    children: List<RTreeNode>,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    doAction: (String, StateID) -> Unit,
) {
    Scaffold(
        topBar = {
            // FIXME we must add options to the top bar
            TopBar(
                action,
                stateID = stateID,
                canPost = canPost,
                onSelect = doAction,
            )
        },
        floatingActionButton = {
            if (Str.notEmpty(stateID.workspace)) {
                FloatingActionButton(
                    onClick = { doAction(AppNames.ACTION_CREATE_FOLDER, stateID) }
                ) { Icon(Icons.Filled.Add, contentDescription = "Create folder") }
            }
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:
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
    children: List<RTreeNode>,
    loadingStatus: LoadingState,
    forceRefresh: () -> Unit,
    open: (StateID) -> Unit,
    modifier: Modifier,
) {
    val ctx = LocalContext.current

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
                    BrowseUpItem(
                        parentDescription,
                        Modifier
                            .fillMaxWidth()
                            .clickable { open(stateID.parent()) }
                    )

                }
            }
            items(children) { oneChild ->
                val isFolder = isFolder(oneChild.mime)
                val currModifier = if (isFolder) {
                    Modifier.clickable { open(oneChild.getStateID()) }
                } else {
                    Modifier
                }

                SelectFolderItem(
                    oneChild,
                    isFolder,
                    mime = oneChild.mime,
                    sortName = oneChild.sortName,
                    title = getNodeTitle(oneChild.name, oneChild.mime),
                    desc = getNodeDesc(ctx, oneChild),
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
    item: RTreeNode,
    isFolder: Boolean,
    mime: String,
    sortName: String?,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    val alpha = if (!isFolder) {
        val outValue = TypedValue()
        LocalContext.current.resources.getValue(R.dimen.disabled_list_item_alpha, outValue, true)
        outValue.float

    } else 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(all = dimensionResource(R.dimen.card_padding))
//            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        Row(
            // modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Thumbnail(item)

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
private fun TopBar(
    action: String,
    stateID: StateID,
    canPost: (StateID) -> Boolean,
    onSelect: (String, StateID) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {

//     val title = stringResource(R.string.choose_target_for_share_title)

    val title = when (action) {
        AppNames.ACTION_UPLOAD -> stringResource(R.string.choose_target_for_share_title)
        AppNames.ACTION_COPY -> stringResource(R.string.choose_target_for_copy_title)
        AppNames.ACTION_MOVE -> stringResource(R.string.choose_target_for_move_title)
        else -> stringResource(R.string.choose_target_subtitle)
    }
    // TODO configure ellipsize from start (or middle?) rather than from the end
    val subTitle = stateID.path ?: "${stateID.username}@${stateID.serverHost}"

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.margin_small),
                    vertical = 0.dp
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

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun TableHeaderPreview() {
    val state = StateID("lucy", "http://example.com", "/all-files/dummy")
    CellsTheme {
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
//    CellsTheme {
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
