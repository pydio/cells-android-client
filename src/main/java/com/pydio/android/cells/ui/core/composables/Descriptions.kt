package com.pydio.android.cells.ui.core.composables

import androidx.compose.runtime.Composable
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import java.util.*

@Composable
fun getJobStatus(item: RJob) : String {

    var desc = "${item.status?.uppercase(Locale.getDefault())} "

    val createdTs = timestampToString(item.creationTimestamp, "dd-MM HH:mm:ss")
    val startTs = timestampToString(item.startTimestamp, "dd-MM HH:mm:ss")
    val updatedTs = timestampToString(item.updateTimestamp, "HH:mm:ss")
    val doneTs = timestampToString(item.doneTimestamp, "dd-MM HH:mm:ss")

    when {
        item.status == AppNames.JOB_STATUS_ERROR ||
                item.status == AppNames.JOB_STATUS_ERROR -> {
            desc += "at $doneTs: ${item.message}"
            // TODO re-enable coloring
            //                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //                    setTextColor(resources.getColor(R.color.danger, context.theme))
            //                }
        }
        item.status == AppNames.JOB_STATUS_CANCELLED -> {
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
            if (currentTimestamp() - item.updateTimestamp < 120) {
                desc += "${asSinceString(item.startTimestamp)}\n${item.progressMessage}"
            } else {
                desc += "idle ${asSinceString(item.updateTimestamp)}\nlast message: ${item.progressMessage}"
            }
        }
        else -> desc += " waiting since $createdTs"
    }
    // Log.e("jobStatus", "Setting status to: $desc")
    return desc
}
