package com.pydio.android.cells.ui.box.browse

import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pydio.android.cells.ui.box.common.BrowseUpItem
import com.pydio.android.cells.ui.box.common.Thumbnail
import com.pydio.android.cells.ui.box.common.getNodeDesc
import com.pydio.android.cells.ui.box.common.getNodeTitle
import com.pydio.android.cells.ui.box.common.isFolder
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "SelectFolder.kt"

@Composable
fun SelectFolderScreen(
    action: String,
    stateId: String,
    isLoading: Boolean,
    browseLocalVM: BrowseLocalFoldersVM,
    openFolder: (StateID) -> Unit,
    openParentDestination: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    postActivity: (StateID, String?) -> Unit,
    forceRefresh: (stateId: StateID) -> Unit,
) {
    val currState by rememberSaveable {
        // This does not work yet: after create is always called and every thing recomposed
        // even when the stateID is unchanged.
        /*derivedStateOf {
            Log.i(logTag, "... Re-computing currState for ${StateID.fromId(stateId)}")
            StateID.fromId(stateId).id
        }
      */  // OR
        mutableStateOf(stateId)
    }

    Log.i(logTag, "... Notified of state change for ${StateID.fromId(currState)}")
    browseLocalVM.setState(StateID.fromId(currState))
    val childNodes by browseLocalVM.childNodes.observeAsState()

    SelectFolderScreen(
        action = action,
        stateID = StateID.fromId(currState),
        children = childNodes ?: listOf(),
        isLoading = isLoading,
        openFolder = openFolder,
        openParentDestination = openParentDestination,
        canPost = canPost,
        postActivity = postActivity,
        forceRefresh = forceRefresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFolderScreen(
    action: String,
    stateID: StateID,
    children: List<RTreeNode>,
    isLoading: Boolean,
    openFolder: (StateID) -> Unit,
    openParentDestination: (StateID) -> Unit,
    canPost: (StateID) -> Boolean,
    postActivity: (StateID, String?) -> Unit,
    forceRefresh: (stateId: StateID) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(action, stateID, canPost, postActivity, Modifier.fillMaxWidth())
        },
        floatingActionButton = {
            if (Str.notEmpty(stateID.workspace)) {
                FloatingActionButton(
                    onClick = { postActivity(stateID, AppNames.ACTION_CREATE_FOLDER) }
                ) { Icon(Icons.Filled.Add, contentDescription = "Create folder") }
            }
        },
    ) { padding -> // Since Compose 1.2.0 it's required to use padding parameter, passed into Scaffold content composable. You should apply it to the topmost container/view in content:
        FolderList(
            action = action,
            stateID = stateID,
            children = children,
            refreshing = isLoading,
            openFolder = openFolder,
            openParent = openParentDestination,
            forceRefresh = forceRefresh,
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
    refreshing: Boolean,
    openFolder: (StateID) -> Unit,
    openParent: (StateID) -> Unit,
    forceRefresh: (stateId: StateID) -> Unit,
    modifier: Modifier,
) {
    val ctx = LocalContext.current

    // var refreshing by remember() { mutableStateOf(isLoading) }
    // Warning: pullRefresh API is:
    //   - experimental
    //   - only implemented in material 1, for the time being.
    Log.d(logTag, "Fist pass, is loading: $refreshing")

    val state = rememberPullRefreshState(refreshing, onRefresh = {
        Log.i(logTag, "Force refresh launched")
        forceRefresh(stateID)
    })

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
                            .clickable { openParent(stateID) }
                    )
                }
            }
            items(children) { oneChild ->

                val isFolder = isFolder(oneChild.mime)
                val currModifier = if (isFolder) {
                    Modifier.clickable {
                        openFolder(StateID.fromId(oneChild.encodedState))
                    }
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
        PullRefreshIndicator(refreshing, state, Modifier.align(Alignment.TopCenter))
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

//            Surface(
//                tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
//                modifier = Modifier
//                    .padding(all = dimensionResource(id = R.dimen.list_thumb_margin))
//                    .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
//            ) {
//                Image(
//                    painter = painterResource(getDrawableFromMime(mime, sortName)),
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(dimensionResource(R.dimen.list_thumb_size))
//                )
//            }

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
    onSelect: (StateID, String?) -> Unit,
    modifier: Modifier
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
                onClick = { onSelect(stateID, action) },
                enabled = canPost(stateID) // Str.notEmpty(stateId.path)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Select this target")
            }
            IconButton(onClick = { onSelect(stateID, AppNames.ACTION_CANCEL) }) {
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
            AppNames.ACTION_UPLOAD,
            state,
            { true },
            { _: StateID, _: String? -> },
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
