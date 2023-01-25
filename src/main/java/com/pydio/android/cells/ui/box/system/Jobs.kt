package com.pydio.android.cells.ui.box.system

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import java.util.*

@Composable
fun JobList(
    jobs: List<RJob>?,
    modifier: Modifier = Modifier
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(jobs ?: listOf()) { job ->
            val jobTitle = "#${job.jobId}: ${job.label}"
            val jobProgress = (job.progress * 100).toFloat().div(job.total)
            JobListItem(
                title = jobTitle,
                status = getJobStatus(job),
                progress = jobProgress,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun JobListItem(
    title: String,
    status: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // TODO add rounded corner on the top right
    // TODO the progress bar part does not appear / disappear when needed.
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {
        Column(
            modifier = modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
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

private fun getJobStatus(job: RJob?): String {
    if (job == null) {
        return "NaN"
    }

    var desc = "${job.status?.uppercase(Locale.getDefault())} "
    val createdTs = timestampToString(job.creationTimestamp, "dd-MM HH:mm:ss")
    val startTs = timestampToString(job.startTimestamp, "dd-MM HH:mm:ss")
    val updatedTs = timestampToString(job.updateTimestamp, "HH:mm:ss")
    val doneTs = timestampToString(job.doneTimestamp, "dd-MM HH:mm:ss")

    when {
        job.status == AppNames.JOB_STATUS_ERROR ||
                job.status == AppNames.JOB_STATUS_ERROR -> {
            desc += "at $doneTs: ${job.message}"
            // TODO re-introduce color management
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
// setTextColor(resources.getColor(R.color.danger, context.theme))
// }
        }
        job.status == AppNames.JOB_STATUS_CANCELLED -> {
            desc += "at $doneTs: ${job.message}"
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                setTextColor(resources.getColor(R.color.colorAccent, context.theme))
//            }
        }

        job.doneTimestamp > 0 -> {
            desc += "at $doneTs: ${job.message}"
        }
        job.startTimestamp > 0 -> {
            if (currentTimestamp() - job.updateTimestamp < 120) {
                desc += "${asSinceString(job.startTimestamp)}\n${job.progressMessage}"
            } else {
                desc += "idle ${asSinceString(job.updateTimestamp)}\nlast message: ${job.progressMessage}"
            }
        }
        else -> desc += " waiting since $createdTs"
    }
    return desc
}


@Preview
@Composable
private fun JobListItemPreview(
) {
    CellsTheme {
        JobListItem("title", "status", 0.7f, Modifier)
    }
}
