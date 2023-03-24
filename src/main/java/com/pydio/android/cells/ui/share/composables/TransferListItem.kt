package com.pydio.android.cells.ui.share.composables

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.core.composables.Decorated
import com.pydio.android.cells.ui.core.composables.Type
import com.pydio.android.cells.ui.core.composables.animations.SmoothLinearProgressIndicator
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Str

// private const val logTag = "TransferListItem"

@Composable
fun TransferListItem(
    item: RTransfer,
    pause: () -> Unit,
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

    val fName = item.getStateId()?.fileName ?: run { "Processing..." }

    TransferListItem(
        item.type,
        item.status ?: AppNames.JOB_STATUS_NEW,
        fName,
        buildStatusString(item),
        progress,
        isActionProcessing = false, // TODO
        pause,
        resume,
        remove,
        more = { more(item.transferId) },
        modifier,
    )
}

fun isFailed(item: RTransfer): Boolean {
    return Str.notEmpty(item.error)
}

fun isPaused(item: RTransfer): Boolean {
    return when (item.status) {
        AppNames.JOB_STATUS_CANCELLED,
        AppNames.JOB_STATUS_CANCELLING,
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
    type: String,
    status: String,
    title: String,
    desc: AnnotatedString,
    progress: Float,
    isActionProcessing: Boolean,
    pause: () -> Unit,
    resume: () -> Unit,
    remove: () -> Unit,
    more: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            //.padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
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
                    SmoothLinearProgressIndicator(
                        indicatorProgress = progress,
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.margin_small))
                    )
                }
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.list_thumb_margin)))

            val btnVectorImg: ImageVector
            val btnModifier: Modifier

            when (status) {
                AppNames.JOB_STATUS_PROCESSING -> {
                    btnVectorImg = CellsIcons.Pause
                    btnModifier = Modifier.clickable { pause() }
                }
                AppNames.JOB_STATUS_CANCELLED,
                AppNames.JOB_STATUS_ERROR,
                -> {
                    btnVectorImg = CellsIcons.Resume
                    btnModifier = Modifier.clickable { resume() }
                }
                AppNames.JOB_STATUS_DONE -> {
                    btnVectorImg = CellsIcons.Delete
                    btnModifier = Modifier.clickable { remove() }
                }
                else -> {
                    btnVectorImg = CellsIcons.Pause
                    btnModifier = Modifier.alpha(0.6f)
                }
            }

            Icon(
                imageVector = btnVectorImg,
                contentDescription = null,
                modifier = btnModifier
                    .size(dimensionResource(R.dimen.list_trailing_icon_size))
            )

//            Surface(modifier = btnModifier) {
//                Icon(
//                    imageVector = btnVectorImg,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(dimensionResource(R.dimen.list_button_size))
//                )
//            }

            val moreModifier = when {
                isActionProcessing -> Modifier.alpha(0.6f)
                else -> Modifier.clickable { more() }
            }

            Surface(modifier = moreModifier) {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }
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
        status = AppNames.JOB_STATUS_CANCELLED,
    )

    CellsTheme {
        TransferListItem(dummyTransfer, { }, { }, { }, { }, Modifier)
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
        status = AppNames.JOB_STATUS_PROCESSING,
    )

    CellsTheme {
        TransferListItem(
            type = AppNames.TRANSFER_TYPE_UPLOAD,
            status = AppNames.JOB_STATUS_PROCESSING,
            title = "Title",
            desc = buildStatusString(dummyTransfer),
            progress = .4f,
            isActionProcessing = false,
            { },
            { },
            { },
            { },
            Modifier,
        )
    }
}
