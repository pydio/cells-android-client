package com.pydio.android.cells.ui.bindings

import android.annotation.SuppressLint
import android.os.Build
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

@BindingAdapter("transferIcon")
fun ImageView.setTransferIcon(item: RTransfer?) {
    if (item == null) {
        return
    }
    setImageResource(
        when (item.type) {
            AppNames.TRANSFER_TYPE_DOWNLOAD -> R.drawable.ic_outline_file_download_24
            else -> R.drawable.ic_outline_file_upload_24
        }
    )
}

@BindingAdapter("transferTitle")
fun TextView.setTransferTitle(item: RTransfer?) {
    item?.let {
        text = item.getStateId().fileName
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("transferText")
fun TextView.setTransferText(item: RTransfer?) {
    item?.let {
        val state = item.getStateId()
//        val arrow = if (item.type == AppNames.TRANSFER_TYPE_UPLOAD) "->" else "<-"
//        text = "${state.fileName} $arrow ${state.username}@${state.serverHost}"
        text = state.fileName
    }
}

@BindingAdapter("transferStatus")
fun TextView.setTransferStatus(item: RTransfer?) {
    item?.let {

        val sizeValue = Formatter.formatShortFileSize(this.context, item.byteSize)
        var desc = "$sizeValue,"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val color = when (item.status) {
                AppNames.JOB_STATUS_WARNING -> R.color.warning
                AppNames.JOB_STATUS_ERROR,
                AppNames.JOB_STATUS_CANCELLED,
                AppNames.JOB_STATUS_TIMEOUT -> R.color.danger
                else -> R.color.material_neutral
            }
            setTextColor(resources.getColor(color, context.theme))
        }

        when {
            Str.notEmpty(item.error) -> {
                desc += " ${item.error}"
            }
            item.doneTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    this.context,
                    item.doneTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                // TODO i18n
                val verb =
                    if (item.type == AppNames.TRANSFER_TYPE_UPLOAD) "uploaded" else "downloaded"
                desc += " $verb on $mTimeValue"
            }
            item.startTimestamp > 0 -> {
                val mTimeValue = DateUtils.formatDateTime(
                    this.context,
                    item.startTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                desc += " started on $mTimeValue"
            }
            else -> {
                val mTimeValue = DateUtils.formatDateTime(
                    this.context,
                    item.creationTimestamp * 1000L,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                desc += " waiting since $mTimeValue"
            }
        }
        text = desc
    }
}

@BindingAdapter("updateTransferProgress")
fun ProgressBar.setTransferProgress(item: RTransfer?) {
    item?.let {
        if (it.byteSize > 0) {
            val percentage = (it.progress * 100) / it.byteSize
            // Log.e("Updating Progress", "New values: ${it.progress} - ${it.byteSize} - $percentage")
            progress = percentage.toInt()
        }
    }
}

@BindingAdapter("showForFailedOnly")
fun View.showForFailedOnly(item: RTransfer?) {
    item?.let {
        visibility = if (Str.notEmpty(item.error)) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}

@BindingAdapter("showForDoneOnly")
fun View.showForDoneOnly(item: RTransfer?) {
    item?.let {
        visibility = if (it.doneTimestamp > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}

@BindingAdapter("parentPrimaryText")
fun TextView.setParentPrimaryText(parentState: StateID?) {
    parentState?.let {
        text = when {
            parentState.id == AppNames.CELLS_ROOT_ENCODED_STATE -> this.resources.getString(R.string.switch_account)
            Str.empty(parentState.workspace) -> this.resources.getString(R.string.switch_workspace)
            else -> ".."
        }
    }
}

@BindingAdapter("parentSecondaryText")
fun TextView.setParentSecondaryText(parentState: StateID?) {
    Log.d("setParentSecondaryText", "ParentState: $parentState")
    parentState?.let {
        text = when {
            parentState.id == AppNames.CELLS_ROOT_ENCODED_STATE -> ""
            Str.empty(parentState.workspace) -> parentState.serverHost
            else -> this.resources.getString(R.string.parent_folder)
        }
    }
}
