package com.pydio.android.cells.ui.core.composables

import android.webkit.MimeTypeMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.core.composables.animations.LoadingAnimation
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.api.SdkNames
import java.io.File

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Thumbnail(item: RTreeNode) {

    val mime = item.mime
    val sortName = item.sortName
    val hasThumb = item.hasThumb()

    if (hasThumb) {
        Surface(
            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
            modifier = Modifier
                .padding(all = dimensionResource(id = R.dimen.list_thumb_margin))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
        ) {
            LoadingAnimation(
                modifier = Modifier.size(dimensionResource(R.dimen.list_thumb_size)),
            )
            GlideImage(
                model = encodeModel(item, AppNames.LOCAL_FILE_TYPE_THUMB),
                contentDescription = "${item.name} thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(dimensionResource(R.dimen.list_thumb_size)),
            )
        }
    } else {
        Surface(
            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
            modifier = Modifier
                .padding(all = dimensionResource(id = R.dimen.list_thumb_margin))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
        ) {
            Image(
                painter = painterResource(getDrawableFromMime(mime, sortName)),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.list_thumb_icon_size))
            )
        }
    }
}

fun betterMime(passedMime: String, sortName: String?): String {
    return if (passedMime == SdkNames.NODE_MIME_DEFAULT) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(File("./$sortName").extension)
            ?: SdkNames.NODE_MIME_DEFAULT
        // newMime = context.contentResolver.getType(Uri.parse("file://"))
    } else passedMime
}

fun isFolder(mime: String): Boolean {

    return mime == SdkNames.WS_TYPE_PERSONAL
            || mime == SdkNames.WS_TYPE_CELL
            || mime == SdkNames.WS_TYPE_DEFAULT
            || mime == SdkNames.NODE_MIME_WS_ROOT
            || mime == SdkNames.NODE_MIME_FOLDER
            || mime == SdkNames.NODE_MIME_RECYCLE
}

fun getDrawableFromMime(originalMime: String, sortName: String?): Int {

    // TODO enrich with more specific icons for files depending on the mime
    val mime = betterMime(originalMime, sortName)

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

fun getWsThumbVector(sortName: String): ImageVector {
    // Tweak: we deduce type of ws root from the sort name. Not very clean
    return when {
        sortName.startsWith("1_2") || sortName.startsWith("2_") -> CellsIcons.MyFilesThumb
        sortName.startsWith("1_8") || sortName.startsWith("8_") -> CellsIcons.CellThumb
        else -> CellsIcons.WorkspaceThumb
    }
}

