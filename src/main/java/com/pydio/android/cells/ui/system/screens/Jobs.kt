package com.pydio.android.cells.ui.system.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.theme.debug
import com.pydio.android.cells.ui.theme.info
import com.pydio.android.cells.ui.theme.warning
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobScreen(
    jobs: List<RJob>,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topAppBarState = rememberTopAppBarState()
    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.job_list_title),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
        modifier = modifier
    ) { innerPadding ->
        JobList(jobs, innerPadding)
    }
}

@Composable
fun JobList(
    jobs: List<RJob>,
    innerPadding: PaddingValues
) {

    LazyColumn(
        Modifier
            .fillMaxWidth()
            .padding(innerPadding)
    ) {
        items(jobs) { job ->

            val jobTitle = "#${job.jobId}: ${job.label}"
            val jobProgress = job.progress.toFloat().div(job.total)

            JobListItem(
                title = jobTitle,
                status = buildStatusString(job),
                progress = jobProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.list_horizontal_padding),
                        vertical = dimensionResource(R.dimen.list_vertical_padding)
                    )
                    .wrapContentWidth(Alignment.Start)
            )
        }
    }
}

@Composable
private fun JobListItem(
    title: String,
    status: AnnotatedString,
    progress: Float,
    modifier: Modifier = Modifier
) {

    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = dimensionResource(R.dimen.list_item_inner_padding))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
            )
            if (progress > 0 && progress < 1) {
                LinearProgressIndicator(progress = progress)
            }
        }
    }
}

@Composable
private fun buildStatusString(job: RJob?): AnnotatedString {

    if (job == null) {
        return buildAnnotatedString { append("NaN") }
    }

    return buildStatusString(
        status = job.status ?: JobStatus.NEW.id,
        creationTimestamp = job.creationTimestamp,
        doneTimestamp = job.doneTimestamp,
        updateTimestamp = job.updateTimestamp,
        startTimestamp = job.startTimestamp,
        message = job.message ?: "",
        progressMessage = job.progressMessage ?: "",
    )
}

@Composable
private fun buildStatusString(
    status: String,
    creationTimestamp: Long,
    doneTimestamp: Long,
    updateTimestamp: Long,
    startTimestamp: Long,
    message: String,
    progressMessage: String,
): AnnotatedString {

    val bgColor = when (status) {
        JobStatus.ERROR.id,
        JobStatus.TIMEOUT.id -> MaterialTheme.colorScheme.error
        JobStatus.WARNING.id,
        JobStatus.CANCELLED.id -> warning
        JobStatus.NEW.id,
        JobStatus.PROCESSING.id -> debug
        else -> info
    }
    val overStyle = SpanStyle(background = bgColor, color = contentColorFor(bgColor))
    val createdTs = timestampToString(creationTimestamp, "dd-MM HH:mm:ss")
    val doneTs = timestampToString(doneTimestamp, "dd-MM HH:mm:ss")

    val text = buildAnnotatedString {
        when {
            status == JobStatus.ERROR.id ||
                    status == JobStatus.TIMEOUT.id -> {
                withStyle(style = overStyle) {
                    append(" ${status.uppercase()} ")
                }
                append(" at $doneTs: $message")
            }
            status == JobStatus.CANCELLED.id -> {
                withStyle(style = overStyle) {
                    append(" ${status.uppercase()} ")
                }
                append(" at $doneTs: $message")
            }
            doneTimestamp > 0 -> {
                withStyle(style = overStyle) {
                    append(" ${status.uppercase()} ")
                }
                append(" at $doneTs: $message")
            }
            startTimestamp > 0 -> {
                withStyle(style = overStyle) {
                    append(" ${status.uppercase()} ")
                }

                if (currentTimestamp() - updateTimestamp < 120) {
                    append(" ${asSinceString(startTimestamp)}\n${progressMessage}")
                } else {
                    append(" idle ${asSinceString(updateTimestamp)}\nlast message: $progressMessage")
                }
            }
            else -> {
                withStyle(style = overStyle) {
                    append(" ${status.uppercase()} ")
                }
                append(" waiting since $createdTs")
            }
        }
    }
    return text
}

@Preview
@Composable
private fun JobListItemPreview(
) {
    val status = buildAnnotatedString {
        append("NaN")
    }

    CellsTheme {
        JobListItem("title", status, 0.7f, Modifier)
    }
}
