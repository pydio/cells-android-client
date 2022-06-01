package com.pydio.android.cells.ui.bindings

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.utils.getTimestampAsENString
import com.pydio.android.cells.utils.timestampToString

@SuppressLint("SetTextI18n")
@BindingAdapter("logTitle")
fun TextView.setLogTitle(item: RLog?) {
    item?.let {
        val ts = timestampToString(item.timestamp, "dd-MM HH:mm:ss")
        val level = item.getLevelString()
        text = "[$level] $ts - ${item.callerId}"
    }
}

@BindingAdapter("logMessage")
fun TextView.setLogMessage(item: RLog?) {
    item?.let {
        text = item.message
    }
}
