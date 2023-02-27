package com.pydio.android.cells.ui.aaLegacy.bindings

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R

@BindingAdapter("offlineBannerIcon")
fun ImageView.setOfflineBannerIcon(type: String?) {
    setImageResource(
        when (type) {
            AppNames.NETWORK_TYPE_METERED -> R.drawable.ic_metered_connection_24
            AppNames.NETWORK_TYPE_ROAMING -> R.drawable.ic_roaming
            else -> R.drawable.ic_baseline_cloud_off_24
        }
    )

    contentDescription = (when (type) {
        AppNames.NETWORK_TYPE_METERED -> resources.getString(R.string.metered_connection)
        AppNames.NETWORK_TYPE_ROAMING -> resources.getString(R.string.roaming_connection)
        else -> resources.getString(R.string.no_internet)
    })
}

@BindingAdapter("offlineBannerDesc")
fun TextView.setOfflineBannerDesc(type: String?) {
    text = (when (type) {
        AppNames.NETWORK_TYPE_METERED -> resources.getString(R.string.metered_connection)
        AppNames.NETWORK_TYPE_ROAMING -> resources.getString(R.string.roaming_connection)
        else -> resources.getString(R.string.no_internet)
    })
}