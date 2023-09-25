package com.pydio.android.cells.ui.browse.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.browse.models.CarouselVM
import com.pydio.android.cells.ui.core.composables.IconThumb
import com.pydio.android.cells.ui.core.composables.LoadingThumb
import com.pydio.cells.transport.StateID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Carousel(
    initialStateID: StateID,
    carouselVM: CarouselVM,
) {
    val filteredItems = carouselVM.preViewableItems.collectAsState(listOf())
    // We have to wait until the list is loaded to display the pager otherwise
    // the scroll-to-page action is ignored
    if (filteredItems.value.isNotEmpty()) {
        HorizontalPagerWithOffsetTransition(
            initialStateID = initialStateID,
            carouselVM,
            filteredItems.value,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerWithOffsetTransition(
    initialStateID: StateID,
    carouselVM: CarouselVM,
    items: List<RTreeNode>,
    modifier: Modifier = Modifier
) {

    val index = remember(key1 = items.size) {
        derivedStateOf {
            getItemIndex(initialStateID, items)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = index.value,
        initialPageOffsetFraction = 0f
    ) {
        items.size
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        key = { currIndex -> items[currIndex].encodedState }
    ) { page -> OneImage(carouselVM.isRemoteLegacy, items, page) }
}

@Composable
@OptIn(ExperimentalGlideComposeApi::class)
private fun OneImage(
    isLegacy: Boolean,
    items: List<RTreeNode>,
    page: Int
) {
    // TODO finalize this: basic example conflicts with the view pager scrolling

    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var angle by remember { mutableFloatStateOf(0f) }

    val imageModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)

    val cardNoZoomModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)

    val cardModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .pointerInput(Unit) {
            detectTransformGestures(
                onGesture = { gestureCentroid, gesturePan, gestureZoom, gestureRotate ->
                    val oldScale = zoom
                    val newScale = (zoom * gestureZoom).coerceIn(1f..10f)

                    // See https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTransformGestures(kotlin.Boolean,kotlin.Function4)
                    // For natural zooming and rotating, the centroid of the gesture should
                    // be the fixed point where zooming and rotating occurs.
                    // We compute where the centroid was (in the pre-transformed coordinate space),
                    // and then compute where it will be after this delta.
                    // We then compute what the new offset should be to keep the centroid
                    // visually stationary for rotating and zooming, and also apply the pan.
                    offset = (offset + gestureCentroid / oldScale).rotateBy(gestureRotate) -
                            (gestureCentroid / newScale + gesturePan / oldScale)
                    angle += gestureRotate
                    zoom = newScale
                }
            )
        }
        .graphicsLayer {
            translationX = fl(offset, zoom)
            translationY = -offset.y * zoom
            scaleX = zoom
            scaleY = zoom
            rotationZ = angle
            TransformOrigin(0f, 0f).also { transformOrigin = it }
        }

    Card(
//        cardModifier
        cardNoZoomModifier
    ) {
        if (isLegacy) {
            GlideImage(
                model = encodeModel(items[page], AppNames.LOCAL_FILE_TYPE_FILE),
                contentDescription = "${items[page].name} thumbnail",
                failure = placeholder {
                    IconThumb(
                        mime = items[page].mime,
                        sortName = items[page].sortName
                    )
                },
                loading = placeholder { LoadingThumb() },
                modifier = imageModifier,
            )
        } else {
            GlideImage(
                model = encodeModel(items[page], AppNames.LOCAL_FILE_TYPE_PREVIEW),
                contentDescription = "${items[page].name} thumbnail",
                loading = placeholder(R.drawable.loading_img),
                failure = placeholder(R.drawable.file_image_outline),
                modifier = imageModifier,
                // TODO we lost this in comparison with XML era
                //   thumbnail(thumbnailRequest)
            )
        }
    }
}

private fun fl(offset: Offset, zoom: Float): Float {
    return -offset.x * zoom
}

fun Offset.rotateBy(angle: Float): Offset {
    val angleInRadians = angle * PI / 180
    return Offset(
        (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
        (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
    )
}

fun getItemIndex(initialStateID: StateID, items: List<RTreeNode>): Int {
    val startingState = initialStateID.id
    items.forEachIndexed { i, node ->
        if (node.encodedState == startingState) {
            return i
        }
    }
    return -1
}
