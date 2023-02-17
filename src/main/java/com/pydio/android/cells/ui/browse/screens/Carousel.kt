package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.transfer.glide.encodeModel
import com.pydio.android.cells.ui.browse.CarouselVM
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel
import kotlin.math.absoluteValue

private const val logTag = "Carousel.kt"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Carousel(
    stateID: StateID,
    back: () -> Unit,
    carouselVM: CarouselVM = koinViewModel(),
) {

    carouselVM.afterCreate(stateID)
    val filteredItems = carouselVM.preViewableItems.observeAsState()

    filteredItems.value?.let {
        Log.d(logTag, "Composing pager with ${it.size} items...")
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Simple Carousel") },
                    backgroundColor = MaterialTheme.colors.surface,
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            HorizontalPagerWithOffsetTransition(
                carouselVM,
                it,
                Modifier.padding(padding)
            )
        }

    }
}

/**
 * Freely inspired from accompanist samples, see: https://github.com/google/accompanist
 */
@OptIn(ExperimentalPagerApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalPagerWithOffsetTransition(
    carouselVM: CarouselVM,
    items: List<RTreeNode>,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        count = items.size,
        // Add 32.dp horizontal padding to 'center' the pages
        contentPadding = PaddingValues(horizontal = 32.dp),
        modifier = modifier.fillMaxSize(),
        key = { index -> items[index].encodedState }
    ) { page ->
        Card(
            Modifier
                .graphicsLayer {
                    // Calculate the absolute offset for the current page from the
                    // scroll position. We use the absolute value which allows us to mirror
                    // any effects for both directions
                    val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                    // We animate the scaleX + scaleY, between 85% and 100%
                    lerp(
                        start = 0.85f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }

                    // We animate the alpha, between 50% and 100%
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box {
                if (carouselVM.isRemoteLegacy) {
                    GlideImage(
                        model = encodeModel(items[page], AppNames.LOCAL_FILE_TYPE_FILE),
                        contentDescription = "${items[page].name} thumbnail",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    GlideImage(
                        model = encodeModel(items[page], AppNames.LOCAL_FILE_TYPE_PREVIEW),
                        contentDescription = "${items[page].name} thumbnail",
                        modifier = Modifier.fillMaxSize(),
                    )
                    // TODO we lost this in comparison with XML era
//                        .placeholder(R.drawable.loading_img)
//                        .error(R.drawable.file_image_outline)
//                        .thumbnail(thumbnailRequest)

                }
            }
        }
    }
}
