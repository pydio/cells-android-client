package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.core.composables.Thumbnail

@Composable
fun GridNodeItem(
    item: RTreeNode,
    title: String,
    desc: String,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    GridNodeItem(
        title = title,
        desc = desc,
        encodedState = item.encodedState,
        name = item.name,
        sortName = item.sortName,
        mime = item.mime,
        eTag = item.etag,
        hasThumb = item.hasThumb(),
        isBookmarked = item.isBookmarked(),
        isOfflineRoot = item.isOfflineRoot(),
        isShared = item.isShared(),
        more = more,
        modifier = modifier,
    )
}

@Composable
fun GridNodeItem(
    title: String,
    desc: String,
    encodedState: String,
    name: String,
    sortName: String?,
    mime: String,
    eTag: String?,
    hasThumb: Boolean,
    isBookmarked: Boolean,
    isOfflineRoot: Boolean,
    isShared: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
    ) {

        Card(
            shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
            ),
            modifier = modifier
        ) {

            Surface(
                tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.glide_thumb_radius)))
                    .wrapContentSize(Alignment.Center)
            ) {
                Thumbnail(encodedState, sortName, name, mime, eTag, hasThumb)
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.grid_ws_content_h_padding),
                )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
