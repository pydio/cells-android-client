package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.getIconAndColorFromType
import com.pydio.android.cells.ui.theme.getIconTypeFromMime
import com.pydio.cells.transport.StateID

// private const val logTag = "GridLargeCard"

@Composable
fun LargeCardWithImage(
    stateID: StateID,
    eTag: String?,
    metaHash: Int,
    mime: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    openMoreMenu: (() -> Unit)? = null,
) {
    LargeCard(title = title, desc = desc, modifier = modifier) {
        LargeCardImageThumb(stateID, eTag, metaHash, mime, openMoreMenu)
    }
}

@Composable
fun LargeCardWithIcon(
    sortName: String?,
    title: String,
    desc: String,
    mime: String,
    modifier: Modifier = Modifier,
    openMoreMenu: (() -> Unit)? = null,
) {
    LargeCard(title = title, desc = desc, modifier = modifier) {
        LargeCardGenericIconThumb(title, mime, sortName, openMoreMenu)
    }
}

@Composable
fun LargeCardGenericIconThumb(
    title: String,
    mime: String,
    sortName: String? = null,
    more: (() -> Unit)? = null,
) {
    getIconAndColorFromType(getIconTypeFromMime(mime, sortName)).let { t ->
        Surface(
            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
            modifier = Modifier
                .fillMaxWidth(1f)
                .size(dimensionResource(R.dimen.grid_ws_image_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
        ) {
            Image(
                painter = painterResource(t.first),
                contentDescription = null,
                colorFilter = ColorFilter.tint(t.second),
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .size(dimensionResource(R.dimen.grid_large_icon_size))
            )
            more?.let {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = "open more menu for $title",
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.grid_large_v_inner_padding),
                            bottom = dimensionResource(R.dimen.grid_large_v_inner_padding),
                            start = dimensionResource(R.dimen.grid_large_v_inner_padding),
                            end = dimensionResource(R.dimen.grid_large_v_inner_padding).div(2)
                        )
                        .wrapContentSize(Alignment.TopEnd)
                        .size(dimensionResource(R.dimen.grid_large_more_size))
                        .clickable { it() }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LargeCardImageThumb(
    stateID: StateID,
    eTag: String?,
    metaHash: Int,
    title: String,
    openMoreMenu: (() -> Unit)? = null,
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
        modifier = Modifier
            .fillMaxWidth(1f)
            .size(dimensionResource(R.dimen.grid_ws_image_size))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
    ) {
        GlideImage(
            model = encodeModel(AppNames.LOCAL_FILE_TYPE_THUMB, stateID, eTag, metaHash),
            contentDescription = "$title thumbnail",
            contentScale = ContentScale.FillWidth,
            failure = placeholder(R.drawable.image_no_thumb_small),
            loading = placeholder(R.drawable.loading),
            modifier = Modifier.size(dimensionResource(id = R.dimen.grid_ws_image_size)),
        )
        openMoreMenu?.let {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopEnd)
                    .clip(
                        RoundedCornerShape(
                            topStart = dimensionResource(R.dimen.grid_large_corner_radius),
                            bottomStart = dimensionResource(R.dimen.grid_large_more_size) * 2,
                            bottomEnd = 2.dp,
                        ),
                    )
                    .background(
                        Brush.horizontalGradient(
                            0.2f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                    .clickable { it() }
            ) {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = "open more menu for $title",
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(id = R.dimen.grid_large_v_inner_padding),
                            bottom = dimensionResource(id = R.dimen.grid_large_v_inner_padding),
                            start = dimensionResource(id = R.dimen.grid_large_v_inner_padding),
                            end = 4.dp
                        )
                        .size(dimensionResource(R.dimen.grid_large_more_size))
                )
            }
        }
    }
}

@Composable
fun LargeCard(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    thumbContent: @Composable () -> Unit,
) {
    val titlePadding = PaddingValues(
        start = dimensionResource(R.dimen.grid_large_h_inner_padding),
        end = dimensionResource(R.dimen.grid_large_h_inner_padding),
        top = dimensionResource(R.dimen.grid_large_v_inner_padding),
        bottom = 2.dp,
    )
    val descPadding = PaddingValues(
        start = dimensionResource(R.dimen.grid_large_h_inner_padding),
        end = dimensionResource(R.dimen.grid_large_h_inner_padding),
        top = 0.dp,
        bottom = dimensionResource(R.dimen.grid_large_v_inner_padding),
    )

    Card(
        shape = RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,// dimensionResource(R.dimen.grid_ws_card_elevation)
        ),
        border = BorderStroke(1.dp, SolidColor(MaterialTheme.colorScheme.outlineVariant)),
        modifier = modifier
    ) {

        thumbContent()

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(titlePadding)
        )
        Text(
            text = desc,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(descPadding)
        )
    }
}
