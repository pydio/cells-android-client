package com.pydio.android.cells.ui.aaLegacy.bindings

//@SuppressLint("SetTextI18n")
//@BindingAdapter("logTitle")
//fun TextView.setLogTitle(item: RLog?) {
//    item?.let {
//        val ts = timestampToString(item.timestamp, "dd-MM HH:mm:ss")
//        val level = item.getLevelString()
//        text = "[$level] $ts - Job #${item.callerId}"
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            when (item.level) {
//                1, 2 -> setTextColor(resources.getColor(R.color.danger, context.theme))
//                3 -> setTextColor(resources.getColor(R.color.colorAccent, context.theme))
//                4 -> setTextColor(resources.getColor(R.color.ok, context.theme))
//                else -> setTextColor(resources.getColor(R.color.material_neutral, context.theme))
//            }
//        }
//    }
//}
//
//@BindingAdapter("logMessage")
//fun TextView.setLogMessage(item: RLog?) {
//    item?.let {
//        text = item.message
//    }
//}
