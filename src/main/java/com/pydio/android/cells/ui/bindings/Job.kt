package com.pydio.android.cells.ui.bindings

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.db.runtime.RJob

//@BindingAdapter("jobIcon")
//fun ImageView.setJobIcon(item: RJob?) {
//    if (item == null) {
//        return
//    }
//    setImageResource(
//        when (item.type) {
//            AppNames.TRANSFER_TYPE_DOWNLOAD -> R.drawable.ic_outline_file_download_24
//            else -> R.drawable.ic_outline_file_upload_24
//        }
//    )
//}

@BindingAdapter("jobTitle")
fun TextView.setJobTitle(item: RJob?) {
    item?.let {
        text = item.label
    }
}

@BindingAdapter("jobStatus")
fun TextView.setJobStatus(item: RJob?) {
    item?.let {
        text = item.status + " - " + item.progressMessage
    }
}

@BindingAdapter("updateJobProgress")
fun ProgressBar.setJobProgress(item: RJob?) {
    item?.let {
        if (it.total < 1){
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