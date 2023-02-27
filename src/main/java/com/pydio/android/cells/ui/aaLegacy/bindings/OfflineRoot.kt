package com.pydio.android.cells.ui.aaLegacy.bindings

import android.annotation.SuppressLint
import android.text.format.Formatter.formatShortFileSize
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.hasTreeNodeFlag
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.utils.asAgoString
import com.pydio.cells.api.SdkNames

@BindingAdapter("offlineRootTitle")
fun TextView.setOfflineRootTitle(item: RLiveOfflineRoot?) {
    item?.let {
        text = if (SdkNames.NODE_MIME_RECYCLE == item.mime) {
            this.resources.getString(R.string.recycle_bin_label)
        } else {
            item.name
        }
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("offlineRootDesc")
fun TextView.setOfflineRootDesc(item: RLiveOfflineRoot?) {

    if (item == null) {
        return
    }

    val prefix = resources.getString(R.string.last_check)

    val mTimeValue = if (item.lastCheckTs > 1) {
        asAgoString(item.lastCheckTs)
    } else {
        resources.getString(R.string.last_check_never)
    }

    val sizeValue = formatShortFileSize(this.context, item.size)
    text = "$prefix: $mTimeValue • $sizeValue"
}

@BindingAdapter("isOffline")
fun SwitchMaterial.setIsOffline(item: RLiveOfflineRoot?) {
    item?.let { isChecked = true }
}

@BindingAdapter("showForOfflineFileOnly")
fun View.setShowForOfflineFileOnly(item: RLiveOfflineRoot?) {
    item?.let { visibility = if (it.isFolder()) View.GONE else View.VISIBLE }
}

@BindingAdapter("offlineRootThumb")
fun ImageView.setOfflineRootThumb(item: RLiveOfflineRoot?) {

    if (item == null) {
        setImageResource(R.drawable.icon_file)
        return
    }

//    if (item.localModificationTS > item.remoteModificationTS) {
//        setImageResource(R.drawable.loading_animation)
//        return
//    }

    if (hasTreeNodeFlag(item.flags, AppNames.FLAG_HAS_THUMB)) {
        Glide.with(context)
            .load(encodeModel(item.encodedState, item.etag, AppNames.LOCAL_FILE_TYPE_THUMB))
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    // RoundedCorners(convertDpToPixel(context, R.dimen.glide_thumb_radius))
                    RoundedCorners(16)
                )
            )
            .into(this)
    } else {
        setImageDrawable(getComposedDrawable(context, item.mime, item.sortName))
    }
}


@BindingAdapter("offlineRootCardThumb")
fun ImageView.setOfflineRootCardThumb(item: RLiveOfflineRoot?) {
    if (item == null) {
        setImageResource(R.drawable.icon_file)
        return
    }

//    if (item.localModificationTS > item.remoteModificationTS) {
//        setImageResource(R.drawable.loading_animation2)
//        return
//    }

    if (hasTreeNodeFlag(item.flags, AppNames.FLAG_HAS_THUMB)) {
        Glide.with(context)
            .load(encodeModel(item.encodedState, item.etag, AppNames.LOCAL_FILE_TYPE_THUMB))
            .transform(CenterCrop())
            .into(this)
    } else {
        setImageDrawable(getComposedDrawableForGrid(context, item.mime, item.sortName))
    }
}
