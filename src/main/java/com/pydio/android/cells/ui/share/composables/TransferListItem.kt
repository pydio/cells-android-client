package com.pydio.android.cells.ui.share.composables

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.core.composables.Decorated
import com.pydio.android.cells.ui.core.composables.Type
import com.pydio.android.cells.ui.core.composables.animations.SmoothLinearProgressIndicator
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.utils.Str

// private const val logTag = "TransferListItem"

@Composable
fun TransferListItem(
    isRemoteLegacy: Boolean,
    item: RTransfer,
    pause: () -> Unit,
    cancel: () -> Unit,
    resume: () -> Unit,
    remove: () -> Unit,
    more: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    val progress = if (item.byteSize > 0) {
        item.progress.toFloat().div(item.byteSize.toFloat())
    } else {
        0f
    }

    val fName = item.getStateID()?.fileName ?: run { "Processing..." }

    TransferListItem(
        isRemoteLegacy = isRemoteLegacy,
        type = item.type,
        status = item.status ?: JobStatus.NEW.id,
        title = fName,
        desc = buildStatusString(item),
        progress = progress,
        isActionProcessing = false, // TODO
        pause = pause,
        cancel = cancel,
        resume = resume,
        remove = remove,
        more = { more(item.transferId) },
        modifier = modifier,
    )
}

fun isFailed(item: RTransfer): Boolean {
    return Str.notEmpty(item.error)
}

fun isPaused(item: RTransfer): Boolean {
    return when (item.status) {
        JobStatus.PAUSED.id,
        JobStatus.PAUSING.id,
        AppNames.UPLOAD_STATUS_LOCALLY_CACHED,
        -> true

        else -> false
    }
}

fun isDone(item: RTransfer): Boolean {
    return item.doneTimestamp > 0
}

@Composable
private fun TransferListItem(
    isRemoteLegacy: Boolean,
    type: String,
    status: String,
    title: String,
    desc: AnnotatedString,
    progress: Float,
    isActionProcessing: Boolean,
    pause: () -> Unit,
    cancel: () -> Unit,
    resume: () -> Unit,
    remove: () -> Unit,
    more: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            horizontal = dimensionResource(R.dimen.list_item_inner_h_padding),
            vertical = dimensionResource(R.dimen.list_item_inner_v_padding),
        )
    ) {

        Decorated(Type.JOB, status) {
            val thumbImg = when (type) {
                AppNames.TRANSFER_TYPE_DOWNLOAD -> CellsIcons.DownloadFile
                else -> CellsIcons.UploadFile
            }
            Icon(
                imageVector = thumbImg,
                contentDescription = null,
                modifier = Modifier
                    .padding(all = dimensionResource(id = R.dimen.list_thumb_padding))
                    .size(dimensionResource(R.dimen.list_thumb_size))
                    .alpha(.5f)
            )
        }

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
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (progress > 0 && progress < 1) {
                SmoothLinearProgressIndicator(
                    indicatorProgress = progress,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.margin_small))
                )
            }
        }

        val btnVectorImg: ImageVector
        val btnModifier: Modifier

        when (status) {
            JobStatus.PROCESSING.id -> {
                if (isRemoteLegacy) {
                    btnVectorImg = CellsIcons.Cancel
                    btnModifier = Modifier.clickable { cancel() }
                } else {
                    btnVectorImg = CellsIcons.Pause
                    btnModifier = Modifier.clickable { pause() }
                }
            }

            JobStatus.PAUSED.id -> {
                btnVectorImg = CellsIcons.Resume
                btnModifier = Modifier.clickable { resume() }
            }

            JobStatus.ERROR.id -> {
                btnVectorImg = CellsIcons.Relaunch
                btnModifier = Modifier.clickable { resume() }
            }

            JobStatus.DONE.id -> {
                btnVectorImg = CellsIcons.Delete
                btnModifier = Modifier.clickable { remove() }
            }

            else -> {
                if (isRemoteLegacy) {
                    btnVectorImg = CellsIcons.Cancel
                    btnModifier = Modifier.clickable { cancel() }
                } else {
                    btnVectorImg = CellsIcons.Pause
                    btnModifier = Modifier.clickable { pause() }
                }
            }
        }

        Icon(
            imageVector = btnVectorImg,
            contentDescription = null,
            modifier = btnModifier
                .requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
        )

        val moreModifier = when {
            isActionProcessing -> Modifier.alpha(0.8f)
            else -> Modifier.clickable { more() }
        }

        Surface(modifier = moreModifier) {
            Icon(
                imageVector = CellsIcons.MoreVert,
                contentDescription = null,
                modifier = Modifier
                    .requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
            )
        }
    }
}


@Composable
fun buildStatusString(item: RTransfer): AnnotatedString {
    val ctx = LocalContext.current
//    val bgColor = when (item.status) {
//        AppNames.JOB_STATUS_WARNING -> CellsColor.warning
//        AppNames.JOB_STATUS_ERROR,
//        AppNames.JOB_STATUS_CANCELLED,
//        AppNames.JOB_STATUS_TIMEOUT -> CellsColor.danger
//        else -> CellsColor.ok
//    }

//     val overStyle = SpanStyle(background = bgColor, color = contentColorFor(bgColor))

    val text = buildAnnotatedString {
        val sizeValue = Formatter.formatShortFileSize(ctx, item.byteSize)
        append("$sizeValue,")
        when {
            Str.notEmpty(item.error) -> {
                append(" ${item.error}")
            }

            item.doneTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx, item.doneTimestamp * 1000L, DateUtils.FORMAT_ABBREV_RELATIVE
                )
                // TODO i18n
                val verb =
                    if (item.type == AppNames.TRANSFER_TYPE_UPLOAD) "uploaded" else "downloaded"
                append(" $verb on $mTimeValue")
            }

            item.startTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx, item.startTimestamp * 1000L, DateUtils.FORMAT_ABBREV_RELATIVE
                )
                append(" started on $mTimeValue")
            }

            else -> {
                val mTimeValue = DateUtils.formatDateTime(
                    ctx, item.creationTimestamp * 1000L, DateUtils.FORMAT_ABBREV_RELATIVE
                )
                append(" waiting since $mTimeValue")
            }
        }
    }
    return text
}

@SuppressLint("SdCardPath")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
    name = "TL Item Light"
)
@Composable
private fun TransferListItemPreview() {

    val dummyTransfer = RTransfer.fromState(
        encodedState = "alice@https%3A%2F%2Fexample.com@%2Fpersonal-files%2FTest%2FIMG_202172836.jpg",
        type = AppNames.TRANSFER_TYPE_UPLOAD,
        path = "/data/user/0/com.pydio.android.Client/files/example.com/local/personal-files/Test/IMG_202172836.jpg",
        byteSize = 13551193L,
        mime = "image/jpeg",
        parentJobId = 0L,
        status = JobStatus.PAUSED.id,
    )

    UseCellsTheme {
        TransferListItem(false, dummyTransfer, { }, { }, { }, { }, { }, Modifier)
    }
}

@SuppressLint("SdCardPath")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "TL Item Dark"
)
@Composable
private fun TransferListItemNightPreview() {

    val dummyTransfer = RTransfer.fromState(
        encodedState = "alice@https%3A%2F%2Fexample.com@%2Fpersonal-files%2FTest%2FIMG_202172836.jpg",
        type = AppNames.TRANSFER_TYPE_UPLOAD,
        path = "/data/user/0/com.pydio.android.Client/files/example.com/local/personal-files/Test/IMG_202172836.jpg",
        byteSize = 13551193L,
        mime = "image/jpeg",
        parentJobId = 0L,
        status = JobStatus.PROCESSING.id,
    )

    UseCellsTheme {
        TransferListItem(
            isRemoteLegacy = false,
            type = AppNames.TRANSFER_TYPE_UPLOAD,
            status = JobStatus.PROCESSING.id,
            title = "Title",
            desc = buildStatusString(dummyTransfer),
            progress = .4f,
            isActionProcessing = false,
            { },
            { },
            { },
            { },
            { },
            Modifier,
        )
    }
}
