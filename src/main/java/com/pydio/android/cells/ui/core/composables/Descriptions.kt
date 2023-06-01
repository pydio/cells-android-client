package com.pydio.android.cells.ui.core.composables

import androidx.compose.runtime.Composable
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import java.util.Locale

@Composable
fun getJobStatus(item: RJob): String {

    // FIXME implement user friendlier messages with i18n

    var desc = "${item.status?.uppercase(Locale.getDefault())} "

    val createdTs = timestampToString(item.creationTimestamp, "dd-MM HH:mm:ss")
//    val startTs = timestampToString(item.startTimestamp, "dd-MM HH:mm:ss")
//    val updatedTs = timestampToString(item.updateTimestamp, "HH:mm:ss")
    val doneTs = timestampToString(item.doneTimestamp, "dd-MM HH:mm:ss")

    when {
        item.status == JobStatus.ERROR.id ||
                item.status == JobStatus.TIMEOUT.id -> {
            desc += "at $doneTs: ${item.message}"
            // TODO re-enable coloring
            //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //                    setTextColor(resources.getColor(R.color.danger, context.theme))
            //                }
        }

        item.status == JobStatus.PAUSED.id -> {
            desc += "at $doneTs: ${item.message}"
            // See above
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                setTextColor(resources.getColor(R.color.colorAccent, context.theme))
//            }
        }

        item.doneTimestamp > 0 -> {
            desc += "at $doneTs: ${item.message}"
        }

        item.startTimestamp > 0 -> {
            desc += if (currentTimestamp() - item.updateTimestamp < 120) {
                "${asSinceString(item.startTimestamp)}\n${item.progressMessage}"
            } else {
                "idle ${asSinceString(item.updateTimestamp)}\nlast message: ${item.progressMessage}"
            }
        }

        else -> desc += " waiting since $createdTs"
    }
    // Log.e("jobStatus", "Setting status to: $desc")
    return desc
}
