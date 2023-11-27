package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.bumptech.glide.integration.compose.GlideSubcomposition
import com.bumptech.glide.integration.compose.RequestState
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.core.composables.animations.LoadingAnimation
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
        LargeCardImageThumb(stateID, eTag, metaHash, title, mime, openMoreMenu)
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
                .size(dimensionResource(R.dimen.grid_image_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
        ) {
            NotSelectedContent(t, more, title)
        }
    }
}

@Composable
private fun NotSelectedContent(
    t: Pair<Int, Color>,
    more: (() -> Unit)?,
    title: String
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LargeCardImageThumb(
    stateID: StateID,
    eTag: String?,
    metaHash: Int,
    title: String,
    mime: String,
    openMoreMenu: (() -> Unit)? = null,
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
        modifier = Modifier
            .fillMaxWidth(1f)
            .size(dimensionResource(R.dimen.grid_ws_image_size))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
    ) {

        GlideSubcomposition(
            encodeModel(AppNames.LOCAL_FILE_TYPE_THUMB, stateID, eTag, metaHash),
            Modifier.size(dimensionResource(R.dimen.grid_ws_image_size)), { it },
        ) {
            when (state) {
                RequestState.Loading -> LoadingAnimation()
                RequestState.Failure -> LargeCardGenericIconThumb(
                    title = title,
                    mime = mime
                ) // IconThumb(mime = mime, sortName = sortName)

                else -> {
                    Image(
                        painter = painter,
                        contentDescription = "$title thumbnail",
                        // modifier = Modifier.size(dimensionResource(R.dimen.grid_ws_image_size)),
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                        alpha = 1f,
                        colorFilter = null,
                    )
                }
            }
        }

//        GlideImage(
//            model = encodeModel(AppNames.LOCAL_FILE_TYPE_THUMB, stateID, eTag, metaHash),
//            contentDescription = "$title thumbnail",
//            contentScale = ContentScale.FillWidth,
//            failure = placeholder(R.drawable.image_no_thumb_small),
//            loading = placeholder(R.drawable.loading),
//            modifier = Modifier.size(dimensionResource(R.dimen.grid_ws_image_size)),
//        )
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
    isSelected: Boolean = false,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomEnd),
        contentAlignment = Alignment.BottomStart,
    ) {
        Card(
            shape = RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(0.8f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) dimensionResource(R.dimen.grid_ws_card_elevation) else 0.dp,
            ),
            border = if (isSelected) {
                BorderStroke(2.dp, SolidColor(MaterialTheme.colorScheme.surfaceTint))
            } else {
                BorderStroke(1.dp, SolidColor(MaterialTheme.colorScheme.outlineVariant))
            },
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
        if (isSelected) {
            Icon(
                imageVector = CellsIcons.Check,
                tint = MaterialTheme.colorScheme.surfaceTint,
                contentDescription = "",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 8.dp)
                    .wrapContentSize(Alignment.BottomEnd)
            )
        }
    }
}


//@Composable
//private fun SelectedContent(
//    t: Pair<Int, Color>,
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth(1f)
//            .size(dimensionResource(R.dimen.grid_image_selected_size))
//            .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
//    ) {
//        Image(
//            painter = painterResource(t.first),
//            contentDescription = null,
//            colorFilter = ColorFilter.tint(t.second),
//            modifier = Modifier
//                .wrapContentSize(Alignment.Center)
//                .size(dimensionResource(R.dimen.grid_large_icon_size))
//        )
//    }
//    Icon(
//        imageVector = CellsIcons.Check,
//        contentDescription = "Selection check",
//        modifier = Modifier
//            .padding(
//                top = dimensionResource(R.dimen.grid_large_v_inner_padding),
//                bottom = dimensionResource(R.dimen.grid_large_v_inner_padding),
//                start = dimensionResource(R.dimen.grid_large_v_inner_padding).div(2),
//                end = dimensionResource(R.dimen.grid_large_v_inner_padding)
//            )
//            .wrapContentSize(Alignment.TopStart)
//            .size(dimensionResource(R.dimen.grid_large_more_size))
//    )
//}

