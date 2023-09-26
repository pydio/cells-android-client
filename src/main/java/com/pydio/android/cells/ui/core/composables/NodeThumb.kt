package com.pydio.android.cells.ui.core.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.pydio.android.cells.ui.theme.getIconAndColorFromType
import com.pydio.android.cells.ui.theme.getIconTypeFromMime
import com.pydio.cells.transport.StateID

@Composable
fun Thumbnail(item: RTreeNode) {
    Thumbnail(
        item.getStateID(),
        item.sortName,
        item.name,
        item.mime,
        item.etag,
        item.metaHash,
        item.hasThumb()
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Thumbnail(
    stateID: StateID,
    sortName: String?,
    name: String,
    mime: String,
    eTag: String?,
    metaHash: Int,
    hasThumb: Boolean,
) {
    if (hasThumb) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
        ) {
            GlideImage(
                model = encodeModel(AppNames.LOCAL_FILE_TYPE_THUMB, stateID, eTag, metaHash),
                contentDescription = "$name thumbnail",
                contentScale = ContentScale.Crop,
                // This is deprecated and has glitches on glide 4.1.16
//                failure = placeholder { IconThumb(mime = mime, sortName = sortName) },
//                loading = placeholder { LoadingThumb() },
                modifier = Modifier.size(dimensionResource(R.dimen.list_thumb_size)),
            )
        }
    } else {
        IconThumb(mime, sortName)
    }
}

@Composable
fun LoadingThumb() {
    LoadingAnimation()
//    Surface(
//        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
//        modifier = Modifier
//            .size(dimensionResource(R.dimen.list_thumb_size))
//            .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
//    ) {
//        Image(
//            painter = painterResource(id),
//            contentDescription = null,
//            colorFilter = ColorFilter.tint(color),
//            modifier = Modifier
//                .wrapContentSize(Alignment.Center)
//                .size(dimensionResource(R.dimen.list_thumb_icon_size))
//        )
//    }
}

@Composable
fun IconThumb(mime: String, sortName: String?) {
    getIconAndColorFromType(getIconTypeFromMime(mime, sortName)).let { t ->
        M3IconThumb(t.first, t.second)
    }
}

@Composable
fun M3IconThumb(@DrawableRes id: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
        modifier = modifier
            .size(dimensionResource(R.dimen.list_thumb_size))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
    ) {
        Image(
            painter = painterResource(id),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .size(dimensionResource(R.dimen.list_thumb_icon_size))
        )
    }
}

//@OptIn(ExperimentalGlideComposeApi::class)
//@Composable
//fun GridThumb(
//    encodedState: String,
//    sortName: String?,
//    name: String,
//    mime: String,
//    eTag: String?,
//    hasThumb: Boolean,
//    outerSize: Dp,
//    iconSize: Dp,
//    padding: PaddingValues = PaddingValues(0.dp),
//    clipShape: Shape,
//) {
//
//    if (hasThumb) {
//        Surface(
//            // tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(padding)
//                .clip(clipShape)
//        ) {
//            LoadingAnimation(
//                modifier = Modifier
//                    .padding(dimensionResource(id = R.dimen.list_thumb_padding))
//                    .size(outerSize),
//            )
//            GlideImage(
//                model = encodeModel(encodedState, eTag, AppNames.LOCAL_FILE_TYPE_THUMB),
//                contentDescription = "$name thumbnail",
//                contentScale = ContentScale.Crop,
//                modifier = Modifier.size(outerSize),
//            )
//        }
//    } else {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(padding)
//                .size(outerSize)
//                .clip(clipShape)
//                .wrapContentSize(Alignment.Center)
//        ) {
//            Icon(
//                painter = painterResource(getDrawableFromMime(mime, sortName, iconSize)),
//                contentDescription = null,
//                modifier = Modifier
//                    .size(iconSize)
//            )
//        }
//    }
//}

//fun getDrawableFromMime(originalMime: String, sortName: String?, iconSize: Dp = 24.dp): Int {
//
//    // TODO enrich with more specific icons for files depending on the mime
//    val mime = betterMime(originalMime, sortName)
//
//    return when {
//        // WS Types
//        mime == SdkNames.WS_TYPE_PERSONAL -> R.drawable.aa_200_folder_shared_48px
//        mime == SdkNames.WS_TYPE_CELL -> R.drawable.file_cells_logo
//        mime == SdkNames.WS_TYPE_DEFAULT -> R.drawable.aa_200_folder_48px
//        mime == SdkNames.NODE_MIME_WS_ROOT -> {
//            // Tweak: we deduce type of ws root from the sort name. Not very clean
//            val prefix = sortName ?: ""
//            when {
//                prefix.startsWith("1_2") -> R.drawable.aa_200_folder_shared_48px
//                prefix.startsWith("1_8") -> R.drawable.file_cells_logo
//                else -> R.drawable.aa_200_folder_48px
//            }
//        }
//        // Folders
//        mime == SdkNames.NODE_MIME_FOLDER -> R.drawable.aa_200_folder_48px
//        mime == SdkNames.NODE_MIME_RECYCLE -> R.drawable.file_trash_outline
//        // Files
//        mime.startsWith("image/", true) -> R.drawable.file_image_outline
//        mime.startsWith("audio/", true) -> R.drawable.ic_outline_audio_file_24
//        mime.startsWith("video/", true) -> R.drawable.ic_outline_video_file_24
//        mime == "application/rtf" || mime == "text/plain"
//        -> R.drawable.file_document_outline
//
//        mime == "application/vnd.oasis.opendocument.text"
//                || mime == "application/msword"
//                || mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
//        -> R.drawable.file_word_outline
//
//        mime == "text/csv" || mime == "application/vnd.ms-excel"
//                || mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
//        -> R.drawable.file_excel_outline
//
//        mime == "application/vnd.oasis.opendocument.presentation"
//                || mime == "application/vnd.ms-powerpoint"
//                || mime == "application/vnd.openxmlformats-officedocument.presentationml.presentation"
//        -> R.drawable.file_powerpoint_outline
//
//        mime == "application/pdf"
//        -> R.drawable.file_pdf_box
//
//        mime == "application/x-httpd-php" ||
//                mime == "application/xml" ||
//                mime == "text/javascript" ||
//                mime == "application/xhtml+xml"
//        -> R.drawable.file_code_outline
//
//        mime == "application/zip" ||
//                mime == "application/x-7z-compressed" ||
//                mime == "application/x-tar" ||
//                mime == "application/java-archive"
//        -> R.drawable.file_zip_outline
//
//        else -> R.drawable.file_outline
//    }
//}

fun getWsThumbVector(sortName: String): ImageVector {
    // Tweak: we deduce type of ws root from the sort name. Not very clean
    return when {
        sortName.startsWith("1_2") || sortName.startsWith("2_") -> CellsIcons.MyFilesThumb
        sortName.startsWith("1_8") || sortName.startsWith("8_") -> CellsIcons.CellThumb
        else -> CellsIcons.WorkspaceThumb
    }
}
