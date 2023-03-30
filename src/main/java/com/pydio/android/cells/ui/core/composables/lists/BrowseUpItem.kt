package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.M3IconThumb

//private const val logTag = "BrowseUpItem"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3BrowseUpListItem(
    parentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
//    Log.d(logTag, "adding the parent row for $parentDescription")
    ListItem(
        headlineContent = { Text(parentDescription) },
        modifier = modifier,
        supportingContent = { Text("..") },
        leadingContent = {
            M3IconThumb(
                R.drawable.aa_200_arrow_back_ios_new_24px,
                color = color
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3BrowseUpLargeGridItem(
    parentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
//    Log.d(logTag, "adding the parent row for $parentDescription")
    LargeCard(title = parentDescription, desc = "..", modifier = modifier) {
        Surface(
            tonalElevation = dimensionResource(R.dimen.list_thumb_elevation),
            modifier = Modifier
                .fillMaxWidth(1f)
                .size(dimensionResource(R.dimen.grid_ws_image_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.grid_large_corner_radius)))
        ) {
            Image(
                painter = painterResource(R.drawable.aa_200_arrow_back_ios_new_24px),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .size(dimensionResource(R.dimen.grid_large_icon_size))
            )
        }
    }
}
