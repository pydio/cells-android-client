package com.pydio.android.cells.ui.browse.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private const val logTag = "Carousel"

@Composable
fun Carousel(
    initialStateID: StateID,
    carouselVM: CarouselVM,
) {
    val filteredItems = carouselVM.preViewableItems.collectAsState(listOf())

    HorizontalPagerWithOffsetTransition(
        initialStateID = initialStateID,
        carouselVM,
        filteredItems.value,
    )
}

/**
 * Freely inspired from accompanist samples, see: https://github.com/google/accompanist
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun HorizontalPagerWithOffsetTransition(
    initialStateID: StateID,
    carouselVM: CarouselVM,
    items: List<RTreeNode>,
    modifier: Modifier = Modifier
) {

    val pagerState = rememberPagerState()

    HorizontalPager(
        pageCount = items.size,
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        key = { index -> items[index].encodedState }
    ) { page ->
        Card(
            Modifier.fillMaxWidth()
        ) {
            Box {
                if (carouselVM.isRemoteLegacy) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                } else {
                    GlideImage(
                        model = encodeModel(items[page], AppNames.LOCAL_FILE_TYPE_PREVIEW),
                        contentDescription = "${items[page].name} thumbnail",
                        loading = placeholder(R.drawable.loading_img),
                        failure = placeholder(R.drawable.file_image_outline),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    // TODO we lost this in comparison with XML era
//                        .thumbnail(thumbnailRequest)
                }
            }
        }
    }

    // scroll to initial page
    LaunchedEffect(key1 = initialStateID) {
        val index = getItemIndex(initialStateID, items)
        if (index >= 0) {
            // Log.e(logTag, "About to scroll to page #$index")
            pagerState.scrollToPage(index)
        } else
            Log.e(logTag, "No index found for $initialStateID")
    }
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
