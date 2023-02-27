package com.pydio.android.cells.ui.aaLegacy.bindings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
import android.text.format.Formatter.formatShortFileSize
import android.util.DisplayMetrics
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.io.File
import kotlin.math.roundToInt

@BindingAdapter("nodeTitle")
fun TextView.setNodeTitle(item: RTreeNode?) {
    item?.let {
        text = if (SdkNames.NODE_MIME_RECYCLE == item.mime) {
            this.resources.getString(R.string.recycle_bin_label)
        } else {
            item.name
        }
    }
}

@BindingAdapter("nodeDesc")
@SuppressLint("SetTextI18n")
fun TextView.setNodeDesc(item: RTreeNode?) {

    if (item == null) {
        return
    }

    if (Str.notEmpty(item.localModificationStatus)) {
        getMessageFromLocalModifStatus(context, item.localModificationStatus!!)?.let {
            text = it
            return
        }
    }

    val mTimeValue = DateUtils.formatDateTime(
        this.context,
        item.remoteModificationTS * 1000L,
        FORMAT_ABBREV_RELATIVE
    )
    val sizeValue = formatShortFileSize(this.context, item.size)
    text = "$mTimeValue • $sizeValue"
}

@BindingAdapter("nodePath")
fun TextView.setNodePath(item: RTreeNode?) {
    item?.let {
        text = item.getStateID().parentPath
    }
}

@BindingAdapter("parentPath")
fun TextView.setParentPath(item: RTreeNode?) {
    item?.let {
        text = StateID.fromId(item.encodedState).parentPath
    }
}

@BindingAdapter("folderThumb")
fun ImageView.setFolderThumb(item: RTreeNode?) {
    if (item == null) {
        return
    }
    setImageDrawable(getComposedDrawable(context, item.mime, item.sortName))
}

@BindingAdapter("nodeThumbLoading")
fun ImageView.showLoadingLayer(item: RTreeNode?) {
    if (item == null) {
        return
    }

    visibility = if (item.localModificationTS > item.remoteModificationTS) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

@BindingAdapter("nodeThumbItem")
fun ImageView.setNodeThumb(item: RTreeNode?) {

    if (item == null) {
        setImageResource(R.drawable.icon_file)
        return
    }

    if (item.hasThumb()) {
        val corner = resources.getDimension(R.dimen.glide_thumb_radius).toInt()
        Glide.with(context)
            .load(encodeModel(item, AppNames.LOCAL_FILE_TYPE_THUMB))
            .placeholder(R.drawable.loading_img)
            .error(R.drawable.no_thumb_for_glide)
            .transform(MultiTransformation(CenterCrop(), RoundedCorners(corner)))
            .into(this)
    } else {
        setImageDrawable(getComposedDrawable(context, item.mime, item.sortName))
    }
}

@BindingAdapter("cardThumbItem")
fun ImageView.setCardThumb(item: RTreeNode?) {
    if (item == null) {
        setImageResource(R.drawable.icon_grid_file)
        return
    }

    if (item.hasThumb()) {
        Glide.with(context)
            .load(encodeModel(item, AppNames.LOCAL_FILE_TYPE_THUMB))
            .placeholder(R.drawable.loading_img)
            .error(R.drawable.no_grid_thumb_for_glide)
            .transform(CenterCrop())
            .into(this)
    } else {
        setImageDrawable(getComposedDrawableForGrid(context, item.mime, item.sortName))
    }
}

@BindingAdapter("multiSelectTitle")
fun TextView.setMultiSelectTitle(size: Int) {
    text = String.format(resources.getQuantityString(R.plurals.selected_count, size), size)
}

@BindingAdapter("offline")
fun ImageView.isOffline(item: RTreeNode) {
    visibility = if (item.isOfflineRoot()) View.VISIBLE else View.GONE
}

@BindingAdapter("bookmark")
fun ImageView.isBookmark(item: RTreeNode) {
    visibility = if (item.isBookmarked()) View.VISIBLE else View.GONE
}

@BindingAdapter("shared")
fun ImageView.isShared(item: RTreeNode) {
    visibility = if (item.isShared()) View.VISIBLE else View.GONE
}

@BindingAdapter("hasPublicLink")
fun SwitchMaterial.setHasPublicLink(item: RTreeNode?) {
    item?.let { isChecked = it.isShared() }
}

@BindingAdapter("isOfflineRoot")
fun SwitchMaterial.setOfflineRoot(item: RTreeNode?) {
    item?.let { isChecked = it.isOfflineRoot() }
}

@BindingAdapter("isBookmarked")
fun SwitchMaterial.setBookmarked(item: RTreeNode?) {
    item?.let { isChecked = it.isBookmarked() }
}

@BindingAdapter("showForFileOnly")
fun View.setShowForFileOnly(item: RTreeNode?) {
    item?.let { visibility = if (it.isFolder()) View.GONE else View.VISIBLE }
}

@BindingAdapter("showForFolderOnly")
fun View.setShowForFolderOnly(item: RTreeNode?) {
    item?.let { visibility = if (it.isFolder()) View.VISIBLE else View.GONE }
}

@BindingAdapter("showForRecycle")
fun View.setShowForRecycle(item: RTreeNode?) {
    item?.let { visibility = if (it.isRecycle()) View.VISIBLE else View.GONE }
}

@BindingAdapter("showForWithinRecycle")
fun View.setShowForWithinRecycle(item: RTreeNode?) {
    item?.let { visibility = if (it.isInRecycle()) View.VISIBLE else View.GONE }
}

fun getComposedDrawable(context: Context, mime: String, sortName: String?): LayerDrawable {
    val background = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.list_icon_background,
        context.theme
    )
    val foreground =
        ResourcesCompat.getDrawable(
            context.resources,
            getDrawableFromMime(mime, sortName),
            context.theme
        )
    val insetPx = context.resources.getDimension(R.dimen.list_thumb_margin).toInt()
    val insetForeground = InsetDrawable(foreground, insetPx, insetPx, insetPx, insetPx)
    return LayerDrawable(arrayOf(background, insetForeground))
}

fun getComposedDrawableForGrid(context: Context, mime: String, sortName: String?): LayerDrawable {
    val background = ResourcesCompat.getDrawable(
        context.resources,
        R.drawable.item_background,
        context.theme
    )
    val foreground =
        ResourcesCompat.getDrawable(
            context.resources,
            getDrawableFromMime(mime, sortName),
            context.theme
        )
    val insetPx = context.resources.getDimension(R.dimen.grid_thumb_margin).toInt()
    // Log.e("getting thumb", "##### compute size: $insetPx ")
    val insetForeground = InsetDrawable(foreground, insetPx, insetPx, insetPx, insetPx)
    return LayerDrawable(arrayOf(background, insetForeground))
}

fun getDrawableFromMime(passedMime: String, sortName: String?): Int {
    // TODO enrich with more specific icons for files depending on the mime

    val mime = if (passedMime == SdkNames.NODE_MIME_DEFAULT) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(File("./$sortName").extension)
            ?: SdkNames.NODE_MIME_DEFAULT
        // newMime = context.contentResolver.getType(Uri.parse("file://"))
    } else passedMime

    return when {
        // WS Types
        mime == SdkNames.WS_TYPE_PERSONAL -> R.drawable.file_folder_shared_outline
        mime == SdkNames.WS_TYPE_CELL -> R.drawable.file_cells_logo
        mime == SdkNames.WS_TYPE_DEFAULT -> R.drawable.file_folder_outline
        mime == SdkNames.NODE_MIME_WS_ROOT -> {
            // Tweak: we deduce type of ws root from the sort name. Not very clean
            val prefix = sortName ?: ""
            when {
                prefix.startsWith("1_2") -> R.drawable.file_folder_shared_outline
                prefix.startsWith("1_8") -> R.drawable.file_cells_logo
                else -> R.drawable.file_folder_outline
            }
        }
        // Folders
        mime == SdkNames.NODE_MIME_FOLDER -> R.drawable.file_folder_outline
        mime == SdkNames.NODE_MIME_RECYCLE -> R.drawable.file_trash_outline
        // Files
        mime.startsWith("image/", true) -> R.drawable.file_image_outline
        mime.startsWith("audio/", true) -> R.drawable.ic_outline_audio_file_24
        mime.startsWith("video/", true) -> R.drawable.ic_outline_video_file_24
        mime == "application/rtf" || mime == "text/plain"
        -> R.drawable.file_document_outline
        mime == "application/vnd.oasis.opendocument.text"
                || mime == "application/msword"
                || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        -> R.drawable.file_word_outline
        mime == "text/csv" || mime == "application/vnd.ms-excel"
                || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        -> R.drawable.file_excel_outline
        mime == "application/vnd.oasis.opendocument.presentation"
                || mime == "application/vnd.ms-powerpoint"
                || mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        -> R.drawable.file_powerpoint_outline
        mime == "application/pdf"
        -> R.drawable.file_pdf_box
        mime == "application/x-httpd-php" ||
                mime == "application/xml" ||
                mime == "text/javascript" ||
                mime == "application/xhtml+xml"
        -> R.drawable.file_code_outline
        mime == "application/zip" ||
                mime == "application/x-7z-compressed" ||
                mime == "application/x-tar" ||
                mime == "application/java-archive"
        -> R.drawable.file_zip_outline
        else -> R.drawable.file_outline
    }
}

fun getMessageFromLocalModifStatus(context: Context, status: String): String? {
    return when (status) {
        AppNames.LOCAL_MODIF_DELETE -> context.getString(R.string.in_progress_deleting)
        AppNames.LOCAL_MODIF_RENAME -> context.getString(R.string.in_progress_renaming)
        AppNames.LOCAL_MODIF_MOVE -> context.getString(R.string.in_progress_moving)
        AppNames.LOCAL_MODIF_RESTORE -> context.getString(R.string.in_progress_restoring)
        else -> null
    }
}

fun convertDpToPixel(context: Context, dp: Float): Int {
    val res =
        dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    return res.roundToInt()
}
