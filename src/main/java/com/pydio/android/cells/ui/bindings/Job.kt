package com.pydio.android.cells.ui.bindings

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.asSinceString
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampToString
import java.util.*

@SuppressLint("SetTextI18n")
@BindingAdapter("jobTitle")
fun TextView.setJobTitle(item: RJob?) {
    item?.let {
        text = "#${it.jobId}: ${item.label}"
    }
}

@BindingAdapter("jobStatus")
fun TextView.setJobStatus(item: RJob?) {
    item?.let {

        var desc = "${item.status?.uppercase(Locale.getDefault())} "

        val createdTs = timestampToString(item.creationTimestamp, "dd-MM HH:mm:ss")
        val startTs = timestampToString(item.startTimestamp, "dd-MM HH:mm:ss")
        val updatedTs = timestampToString(item.updateTimestamp, "HH:mm:ss")
        val doneTs = timestampToString(item.doneTimestamp, "dd-MM HH:mm:ss")

        when {
            item.status == AppNames.JOB_STATUS_ERROR ||
                    item.status == AppNames.JOB_STATUS_ERROR -> {
                desc += "at $doneTs: ${item.message}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextColor(resources.getColor(R.color.danger, context.theme))
                }
            }
            item.status == AppNames.JOB_STATUS_CANCELLED -> {
                desc += "at $doneTs: ${item.message}"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextColor(resources.getColor(R.color.colorAccent, context.theme))
                }
            }

            item.doneTimestamp > 0 -> {
                desc += "at $doneTs: ${item.message}"
            }
            item.startTimestamp > 0 -> {
                if (currentTimestamp() - item.updateTimestamp < 120){
                    desc += "${asSinceString(item.startTimestamp)}\n${item.progressMessage}"
                } else  {
                    desc += "idle ${asSinceString(item.updateTimestamp)}\nlast message: ${item.progressMessage}"
                }
            }
            else -> desc += " waiting since $createdTs"
        }
        // Log.e("jobStatus", "Setting status to: $desc")
        text = desc
    }
}

@BindingAdapter("updateJobProgress")
fun ProgressBar.setJobProgress(item: RJob?) {
    item?.let {
        if (it.total < 1) {
            return
        }
        val percentage = (it.progress * 100) / it.total
        // Log.e("Updating Progress", "New values: ${it.progress} - ${it.byteSize} - $percentage")
        progress = percentage.toInt()
    }
}

@BindingAdapter("showForFailedOnly")
fun View.showForFailedOnly(item: RJob?) {
    item?.let {
        visibility = if (item.isFail()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}

@BindingAdapter("showForDoneOnly")
fun View.showForDoneOnly(item: RJob?) {
    item?.let {
        visibility = if (it.doneTimestamp > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}