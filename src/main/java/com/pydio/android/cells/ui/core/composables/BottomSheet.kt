package com.pydio.android.cells.ui.core.composables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons


/** Data for a given item in the BottomSheet */

interface IMenuItem {
    fun onClick()
}

class SimpleMenuItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
) : IMenuItem {
    override fun onClick() {
        onClick()
    }
}

@Composable
fun BottomSheetContent(
    header: @Composable () -> Unit,
    simpleMenuItems: List<SimpleMenuItem>,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bgColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            header()
        }
        item {
            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.bottom_sheet_h_padding))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .6f),
                thickness = 1.dp,
            )
        }
        items(simpleMenuItems) { item ->
            BottomSheetListItem(
                icon = item.icon,
                title = item.title,
                onItemClick = item.onClick,
                tint = tint,
                bgColor = bgColor,
            )
        }
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.bottom_sheet_v_padding)),
            )
        }
    }
}

@Composable
fun BottomSheetHeader(
    icon: ImageVector,
    title: String,
    desc: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bgColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    BottomSheetHeader(
        thumb = { Icon(imageVector = icon, contentDescription = title, tint = tint) },
        title = title,
        desc = desc,
        tint,
        bgColor,
    )
}

@Composable
fun BottomSheetHeader(
    thumb: @Composable () -> Unit,
    title: String,
    desc: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bgColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.bottom_sheet_h_padding),
                    vertical = dimensionResource(R.dimen.bottom_sheet_v_padding),
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            thumb()
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.item_spacer_width)))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
                    color = tint,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = desc,
                    color = tint,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun BottomSheetListItem(
    icon: ImageVector,
    title: String,
    tint: Color,
    bgColor: Color,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .height(dimensionResource(R.dimen.bottom_sheet_item_height))
            .background(color = bgColor)
            .padding(
                horizontal = dimensionResource(R.dimen.bottom_sheet_h_padding),
//                vertical = dimensionResource(R.dimen.bottom_sheet_v_padding),
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(imageVector = icon, contentDescription = title, tint = tint)
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.item_spacer_width)))
        Text(text = title, color = tint)
    }
}

@Preview(showBackground = true)
@Composable
fun BottomSheetContentPreview() {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current
    val onClick: (String) -> Unit = { title ->
        Toast.makeText(
            context, title, Toast.LENGTH_SHORT
        ).show()
    }
    val simpleMenuItems: List<SimpleMenuItem> = listOf(
        SimpleMenuItem(CellsIcons.Share, "Share") { onClick("Share") },
        SimpleMenuItem(CellsIcons.Link, "Get Link") { onClick("Get Link") },
        SimpleMenuItem(CellsIcons.Edit, "Edit") { onClick("Edit") },
        SimpleMenuItem(CellsIcons.Delete, "Delete") { onClick("Delete") },
    )

    BottomSheetContent(
        {
            BottomSheetHeader(
                icon = CellsIcons.Processing,
                title = "My Transfer of jpg.pdf",
                desc = "45MB, started at 5.54 AM, 46% done",
                tint = tint,
                bgColor = bg,
            )
        },
        simpleMenuItems,
        tint = tint,
        bgColor = bg,
    )
}

@Preview(showBackground = true)
@Composable
fun BottomSheetListItemPreview() {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    BottomSheetListItem(
        icon = CellsIcons.Processing,
        title = "Share",
        onItemClick = { },
        tint = tint,
        bgColor = bg,
    )
}
