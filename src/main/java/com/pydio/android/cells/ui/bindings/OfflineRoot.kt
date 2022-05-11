package com.pydio.android.cells.ui.bindings

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
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
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.cells.api.SdkNames
import com.pydio.cells.utils.Str
import java.io.File

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
        DateUtils.formatDateTime(
            this.context,
            item.lastCheckTs * 1000L,
            FORMAT_ABBREV_RELATIVE
        )
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


@BindingAdapter("offlineRootThumb", "nodeThumbDirPath")
fun ImageView.setOfflineRootThumb(item: RLiveOfflineRoot?, thumbDirPath: String?) {

    if (item == null) {
        setImageResource(R.drawable.icon_file)
        return
    }

//    if (item.localModificationTS > item.remoteModificationTS) {
//        setImageResource(R.drawable.loading_animation)
//        return
//    }

    var thumbPath: String? = null
    if (Str.notEmpty(thumbDirPath) && Str.notEmpty(item.thumbFilename)) {
        // TODO make this generic
        thumbPath = "$thumbDirPath/${item.thumbFilename}"
    }
    if (thumbPath != null && File(thumbPath).exists()) {
        Glide.with(context)
            .load(File(thumbPath))
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    // TODO Directly getting  the radius with R fails => image is a circle
                    // RoundedCorners(R.dimen.glide_thumb_radius)
                    RoundedCorners(16)
                )
            )
            .into(this)
    } else {
        // Log.d("offlineRootThumb", "no thumb found for ${item.name} @ $thumbPath")
//        setImageResource(getDrawableFromMime(item.mime, item.sortName))
        setImageDrawable(getComposedDrawable(context, item.mime, item.sortName))
    }
}


@BindingAdapter("offlineRootCardThumb", "nodeThumbDirPath")
fun ImageView.setOfflineRootCardThumb(item: RLiveOfflineRoot?, thumbDirPath: String?) {
    if (item == null) {
        setImageResource(R.drawable.icon_file)
        return
    }

//    if (item.localModificationTS > item.remoteModificationTS) {
//        setImageResource(R.drawable.loading_animation2)
//        return
//    }

    var thumbPath: String? = null
    if (Str.notEmpty(thumbDirPath) && Str.notEmpty(item.thumbFilename)) {
        // TODO make this generic
        thumbPath = "$thumbDirPath/${item.thumbFilename}"
    }
    if (thumbPath != null && File(thumbPath).exists()) {
        Glide.with(context)
            .load(File(thumbPath))
            .transform(CenterCrop())
            .into(this)
    } else {
        setImageDrawable(getComposedDrawable(context, item.mime, item.sortName))
    }
}


