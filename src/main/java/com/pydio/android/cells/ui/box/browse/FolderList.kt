package com.pydio.android.cells.ui.box.browse

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.bindings.getMessageFromLocalModifStatus
import com.pydio.android.cells.ui.model.BrowseLocalFolders
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "FolderList.kt"

@Composable
fun FolderList(
    action: String,
    stateId: String,
    isLoading: Boolean,
    browseLocalVM: BrowseLocalFolders,
    openFolder: (StateID) -> Unit,
    openParentDestination: (StateID) -> Unit,
    postActivity: (StateID) -> Unit,
    cancelActivity: () -> Unit,
) {

    val currState by rememberSaveable {
        mutableStateOf(stateId)
    }
    browseLocalVM.setState(StateID.fromId(currState))
    val childNodes by browseLocalVM.childNodes.observeAsState()

    FolderList(
        action = action,
        stateId = stateId,
        children = childNodes ?: listOf(),
        isLoading = isLoading,
        openFolder = openFolder,
        openParentDestination = openParentDestination,
        postActivity = postActivity,
        cancelActivity = cancelActivity,
    )
}

@Composable
fun FolderList(
    action: String,
    stateId: String,
    children: List<RTreeNode>,
    isLoading: Boolean,
    // pickFolderVM: PickFolderViewModel,
    openFolder: (StateID) -> Unit,
    openParentDestination: (StateID) -> Unit,
    postActivity: (StateID) -> Unit,
    cancelActivity: () -> Unit,
) {

    val ctx = LocalContext.current

    // This does not work yet: after create is always called and every thing recomposed
    // even when the stateID is unchanged.
    val currState by remember {
        derivedStateOf {
            Log.d(logTag, "Calculing")
            StateID.fromId(stateId).id
        }
    }

    // SPEC:

    // ViewModel correctly scoped with injected services and:
    // - MutableStateOf StateID
    // - FlowOf LiveData for this state id
    // - Background worker that regularly triggers remote fetch of data
    //    - with backoff
    //    - with cancel (?)
    //    - with reset backoff <=> force flag

    val open: (stateID: StateID) -> Unit = {
        // TODO VM Set stateID
        // vm.setStateID(stateID)
        openFolder(it)
    }

    val post: (StateID) -> Unit = {
        postActivity(it)
    }

    Column {
        TableHeader(
            action,
            StateID.fromId(currState),
            postActivity,
            cancelActivity,
            Modifier.fillMaxWidth()
        )
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    ParentListItem(
                        StateID.fromId(currState),
                        Modifier.clickable {
                            openParentDestination(StateID.fromId(currState))
                        })
                }
                items(children) { oneChild ->
                    SelectTargetListItem(
                        title = getTargetTitle(oneChild.name, oneChild.mime),
                        desc = getTargetDesc(ctx, oneChild),
                        modifier = Modifier.clickable {
                            openFolder(StateID.fromId(oneChild.encodedState))
                        }
                    )
                }
            }
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.BottomCenter)
                ) {
                    CircularProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(R.dimen.margin_medium))
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}


//private class StateIdSaver : Saver<StateID, String> {
//    override fun SaverScope.save(value: StateID): String? {
//        return value.id
//    }
//
//    override fun restore(value: String): StateID? {
//        return StateID.fromId(value)
//    }
//}

@Composable
private fun TableHeader(
    action: String,
    stateId: StateID,
    onSelect: (StateID) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier
) {

    val title = when (action) {
        AppNames.ACTION_UPLOAD -> stringResource(R.string.choose_target_for_share_title)
        AppNames.ACTION_COPY -> stringResource(R.string.choose_target_for_copy_title)
        AppNames.ACTION_MOVE -> stringResource(R.string.choose_target_for_move_title)
        else -> stringResource(R.string.choose_target_subtitle)
    }
    // TODO configure ellipsize from start (or middle?) rather than from the end
    val subTitle = stateId.path ?: "${stateId.username}@${stateId.serverHost}"

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.margin_small),
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
                onClick = { onSelect(stateId) },
                enabled = Str.notEmpty(stateId.path)
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Select this target")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel activity")
            }
        }
    }
}

@Composable
private fun ParentListItem(
    stateID: StateID,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        val parentDescription = when {
            Str.empty(stateID.path) -> stringResource(id = R.string.switch_account)
            Str.empty(stateID.fileName) -> stringResource(id = R.string.switch_workspace)
            else -> stringResource(R.string.parent_folder)
        }


        Row(modifier = Modifier.padding(all = 8.dp)) {

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    // .size(dimensionResource(R.dimen.list_thumb_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_new_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .size(48.dp)
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        //.clip(CircleShape)
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

@Composable
private fun SelectTargetListItem(
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            Surface(
                tonalElevation = dimensionResource(R.dimen.list_item_elevation),
                modifier = Modifier
                    .size(40.dp)
                    // .size(dimensionResource(R.dimen.list_thumb_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
            ) {
                Image(
                    painter = painterResource(R.drawable.file_folder_outline),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .size(48.dp)
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        //.clip(CircleShape)
                        .wrapContentSize(Alignment.Center)
                )
//                Image(
//                    painter = painterResource(R.drawable.ic_baseline_check_24),
//                    contentDescription = null,
//                    colorFilter = ColorFilter.tint(Color.Green),
//                    modifier = Modifier // .fillMaxSize()
//                        //.size(dimensionResource(R.dimen.list_thumb_decorator_size))
//                        .size(12.dp)
//                        .wrapContentSize(Alignment.BottomEnd)
//                )
            }

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

private fun getTargetTitle(name: String, mime: String): String {
    return if (SdkNames.NODE_MIME_RECYCLE == mime) {
        // Todo rather use a composable here to have ressources
        "Recycle Bin"
    } else {
        name
    }
}

private fun getTargetDesc(
    ctx: Context,
    item: RTreeNode?
): String {
    if (item == null) {
        return "NaN"
    }
    if (Str.notEmpty(item.localModificationStatus)) {
        getMessageFromLocalModifStatus(ctx, item.localModificationStatus!!)?.let {
            return it
        }
    }
    val mTimeValue = DateUtils.formatDateTime(
        ctx,
        item.remoteModificationTS * 1000L,
        DateUtils.FORMAT_ABBREV_RELATIVE
    )
    val sizeValue = Formatter.formatShortFileSize(ctx, item.size)
    return "$mTimeValue • $sizeValue"
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun TableHeaderPreview() {
    CellsTheme {
        TableHeader(
            AppNames.ACTION_UPLOAD,
            StateID("jack", "http://example.com", "/all-files/dummy"),
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
private fun AccountListItemPreview() {
    CellsTheme {
        SelectTargetListItem("WS on encrypted", "29 October 2020 • 81 MB", Modifier)
    }
}
