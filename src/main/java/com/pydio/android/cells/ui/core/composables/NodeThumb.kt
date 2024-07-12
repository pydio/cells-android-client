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
import com.pydio.android.cells.ui.models.TreeNodeItem
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

@Composable
fun Thumbnail(item: TreeNodeItem) {
    Thumbnail(
        item.stateID,
        item.sortName,
        item.name,
        item.mime,
        item.eTag,
        item.metaHash,
        item.hasThumb,
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
                contentDescription = "Thumb for $name",
                modifier = Modifier.size(dimensionResource(R.dimen.list_thumb_size)),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
            ) {
                // TODO find a way to display an animated loading thumb during Glide loading
                it.error(R.drawable.image_no_thumb_small)
                    .placeholder(R.drawable.image_no_thumb_small)
            }
        }
    } else {
        IconThumb(mime, sortName)
    }
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

fun getWsThumbVector(sortName: String): ImageVector {
    // Tweak: we deduce type of ws root from the sort name. Not very clean
    return when {
        sortName.startsWith("1_2") || sortName.startsWith("2_") -> CellsIcons.MyFilesThumb
        sortName.startsWith("1_8") || sortName.startsWith("8_") -> CellsIcons.CellThumb
        else -> CellsIcons.WorkspaceThumb
    }
}
