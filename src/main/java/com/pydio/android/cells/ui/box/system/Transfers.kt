package com.pydio.android.cells.ui.box.system

import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.theme.danger
import com.pydio.android.cells.ui.theme.ok
import com.pydio.android.cells.ui.theme.warning
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "Transfers.kt"

@Composable
fun UploadProgressScreen(
    stateID: StateID,
    uploads: List<RTransfer>,
    dismiss: () -> Unit,
) {
    Column {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(uploads) { upload ->
                TransferListItem(
                    upload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.card_padding))
                        .wrapContentWidth(Alignment.Start)
                )
            }
        }

        TextButton(
            onClick = {
                // Toast.makeText(ctx, "Cancel button clicked!!", Toast.LENGTH_LONG).show()
                dismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(R.dimen.margin))
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) { Text(stringResource(R.string.button_run_in_background)) }
    }
}

@Composable
private fun TransferListItem(
    item: RTransfer,
    modifier: Modifier = Modifier
) {

    val progress = if (item.byteSize > 0) {
        item.progress.toFloat().div(item.byteSize.toFloat())
    } else {
        0f
    }

    val fName = item.getStateId().fileName ?: run {
        // FIXME it seems that we have a bug in the state ID generation:
        // user@https://example.com/Baloon.jpg <- we are missing the workspace
        Log.e(logTag, "no filename for ${item.getStateId()}")
        "-"
    }

    TransferListItem(
        item.type,
        fName,
        buildStatusString(item),
        progress,
        modifier
    )
}

@Composable
private fun TransferListItem(
    type: String,
    title: String,
    desc: AnnotatedString,
    progress: Float,
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
                    painter = painterResource(
                        when (type) {
                            AppNames.TRANSFER_TYPE_DOWNLOAD -> R.drawable.ic_outline_file_download_24
                            else -> R.drawable.ic_outline_file_upload_24
                        }
                    ),
                    contentDescription = null,
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
                    .weight(1f)
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
                if (progress > 0 && progress < 1) {
                    LinearProgressIndicator(progress = progress)
                }
            }

//            if (authStatus != AppNames.AUTH_STATUS_CONNECTED) {
//
//                Surface(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
//                        .clickable(onClick = {
//                            doLogin(StateID(login, url))
//                        })
//                        .background(MaterialTheme.colorScheme.error)
//                ) {
//                    Image(
//                        painter = painterResource(R.drawable.ic_baseline_login_24),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .size(48.dp)
//                            .size(dimensionResource(R.dimen.list_thumb_size))
//                            //.clip(CircleShape)
//                            .wrapContentSize(Alignment.Center)
//                    )
//                }
//            }
        }
    }
}


@Composable
private fun buildStatusString(item: RTransfer): AnnotatedString {
    val ctx = LocalContext.current
    val bgColor = when (item.status) {
        AppNames.JOB_STATUS_WARNING -> warning
        AppNames.JOB_STATUS_ERROR,
        AppNames.JOB_STATUS_CANCELLED,
        AppNames.JOB_STATUS_TIMEOUT -> danger
        else -> ok
    }
    val overStyle = SpanStyle(background = bgColor, color = contentColorFor(bgColor))

    val text = buildAnnotatedString {
        val sizeValue = Formatter.formatShortFileSize(ctx, item.byteSize)
        append("$sizeValue,")
        when {
            Str.notEmpty(item.error) -> {
                append(" ${item.error}")
            }
            item.doneTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx,
                    item.doneTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                // TODO i18n
                val verb =
                    if (item.type == AppNames.TRANSFER_TYPE_UPLOAD) "uploaded" else "downloaded"
                append(" $verb on $mTimeValue")
            }
            item.startTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx,
                    item.startTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                append(" started on $mTimeValue")
            }
            else -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx,
                    item.creationTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                append(" waiting since $mTimeValue")
            }
        }
    }
    return text
}


fun isFailed(item: RTransfer): Boolean {
    return Str.notEmpty(item.error)
}

fun isDone(item: RTransfer): Boolean {
    return item.doneTimestamp > 0
}
