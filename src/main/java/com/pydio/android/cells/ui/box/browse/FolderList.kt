package com.pydio.android.cells.ui.box.browse

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.bindings.getMessageFromLocalModifStatus
import com.pydio.android.cells.ui.model.SelectTargetViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.transfer.PickFolderViewModel
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

@Composable
fun FolderList(
    stateID: StateID,
    selectTargetVM: SelectTargetViewModel,
    pickFolderVM: PickFolderViewModel,
    openFolder: (stateID: StateID) -> Unit,
    openParentDestination: (stateID: StateID) -> Unit,
    modifier: Modifier,
) {

    val ctx = LocalContext.current
    pickFolderVM.afterCreate(stateID)
    val children by pickFolderVM.children.observeAsState()

    LazyColumn(Modifier.fillMaxWidth()) {
        items(children ?: listOf()) { oneChild ->
            SelectTargetListItem(
                title = getTargetTitle(oneChild.name, oneChild.mime),
                desc = getTargetDesc(ctx, oneChild),
                modifier = modifier.clickable {
                    openFolder(StateID.fromId(oneChild.encodedState))
                }
            )
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
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        Row(modifier = Modifier.padding(all = 8.dp)) {

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    // .size(dimensionResource(R.dimen.list_thumb_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_person_24),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .size(48.dp)
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        //.clip(CircleShape)
                        .wrapContentSize(Alignment.Center)
                )
                Image(
                    painter = painterResource(R.drawable.ic_baseline_check_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Green),
                    modifier = Modifier // .fillMaxSize()
                        //.size(dimensionResource(R.dimen.list_thumb_decorator_size))
                        .size(12.dp)
                        .wrapContentSize(Alignment.BottomEnd)
                )
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
private fun AccountListItemPreview(
) {
    CellsTheme {
        SelectTargetListItem("WS on encrypted", "29 October 2020 • 81 MB", Modifier)
    }
}
