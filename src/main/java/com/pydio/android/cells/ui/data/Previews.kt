package com.pydio.android.cells.ui.data

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme

@Composable
fun DummyImage200() {
    Box(modifier = Modifier.size(200.dp)) {
        Icon(
            imageVector = CellsIcons.Processing,
            contentDescription = null,
            tint = CellsColor.flagOffline.copy(alpha = .5f),
            modifier = Modifier
                .size(200.dp)
                .background(CellsColor.flagShare.copy(alpha = .3f))
                .align(Alignment.Center)
        )
        Icon(
            imageVector = CellsIcons.Bookmark,
            contentDescription = null,
            tint = CellsColor.warning,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .size(96.dp)
                .background(CellsColor.warning.copy(alpha = .3f))
                .align(Alignment.BottomEnd)
        )
    }
}

@Preview("Thumb Icon light")
@Composable
private fun ThumbIconPreview() {
    CellsTheme {
        DummyImage200()
    }
}


@Preview
@Composable
private fun NewNodeCardPreview() {

    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 4.dp,
    )

    val title = "IMG_20220508_172716.jpg"
    val isBookmarked = true
    val isOfflineRoot = true
    val isShared = true

    val more: () -> Unit = {}


    CellsTheme {
        Box(modifier = Modifier.size(200.dp)) {

            DummyImage200()

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f))
                    .padding(titlePadding)
            )

            Column(Modifier.padding(all = 4.dp)) {
                if (isBookmarked) {
                    Image(
                        painter = painterResource(R.drawable.ic_baseline_star_border_24),
                        colorFilter = ColorFilter.tint(CellsColor.flagBookmark),
                        modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator)),
                        contentDescription = ""
                        // contentScale = ContentScale.Crop,
                    )
                }
                if (isShared) {
                    Image(
                        painter = painterResource(R.drawable.ic_baseline_link_24),
                        colorFilter = ColorFilter.tint(CellsColor.flagShare),
                        contentDescription = "",
                        modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
                    )
                }
                if (isOfflineRoot) {
                    Image(
                        painter = painterResource(R.drawable.ic_outline_download_done_24),
                        colorFilter = ColorFilter.tint(CellsColor.flagOffline),
                        contentDescription = "",
                        modifier = Modifier.size(dimensionResource(R.dimen.list_item_flag_decorator))
                    )
                }
            }

            IconButton(
                onClick = { more() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = CellsIcons.MoreVert,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_button_size)),
                )
            }
        }
    }
}


@Preview
@Composable
private fun NodeCardPreview() {

    val titlePadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 4.dp,
        bottom = 0.dp,
    )
    val descPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 0.dp,
        bottom = 8.dp,
    )

    val title = "IMG_20220508_172716.jpg"
    val desc = "November 14, 2022 • 2.0 MB"

    CellsTheme {
        Card(
            shape = RoundedCornerShape(dimensionResource(R.dimen.grid_ws_image_corner_radius)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = dimensionResource(R.dimen.grid_ws_card_elevation)
            ),
            modifier = Modifier
        ) {
            DummyImage200()
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(titlePadding)
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(descPadding)
            )
        }
    }
}

// Seems like drawable jpg cannot be used in previews
//@Preview("ThumbRes preview light")
//@Composable
//private fun ThumbResourcePreview() {
//    CellsTheme {
//        Image(
//            painter = painterResource(R.drawable.dog),
//            contentDescription = "Image",
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//                .padding(10.dp)
//        )
//    }
//}

// does not work either

//@Preview("Thumb preview light")
//@Composable
//private fun ThumbPreview() {
//    CellsTheme {
//        // This must be manually put on the test device...
//        val imgFile = File("/storage/emulated/0/Download/lavande.jpeg")
//
//        var imgBitmap: Bitmap? = null
//        if (imgFile.exists()) {
//            imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
//        } else {
//            Text("No file")
//        }
//
//        imgBitmap?.let {
//            Image(
//                bitmap = imgBitmap.asImageBitmap(),
//                contentDescription = "Image",
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp)
//                    .padding(10.dp)
//            )
//        }
//    }
//}
