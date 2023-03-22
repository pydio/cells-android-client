package com.pydio.android.cells.ui.core.composables

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuData
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.core.composables.menus.SimpleMenuItem
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetLayout
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellsModalBottomSheetLayout(
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    sheetState: ModalBottomSheetState,
    launch: (NodeAction) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetContent = { NodeMoreMenuData(type, toOpenStateID, launch) },
        modifier = Modifier,
        sheetState = sheetState,
        sheetElevation = 3.dp,
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        content = content,
    )
}

@Composable
fun BottomSheetContent(
    header: @Composable () -> Unit,
    simpleMenuItems: List<SimpleMenuItem>,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = dimensionResource(id = R.dimen.bottom_sheet_v_spacing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            header()
        }
        item {
            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(R.dimen.bottom_sheet_item_h_padding))
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant, //.copy(alpha = .6f),
                thickness = 1.dp,
            )
        }
        items(simpleMenuItems) { item ->
            BottomSheetListItem(
                icon = item.icon,
                title = item.title,
                onItemClick = item.onClick,
                selected = item.selected,
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
//    tint: Color = MaterialTheme.colorScheme.onSurface,
//    bgColor: Color = MaterialTheme.colorScheme.surface,
) {
    BottomSheetHeader(
        thumb = { Icon(imageVector = icon, contentDescription = title) },
        title = title,
        desc = desc,
//        tint,
//        bgColor,
    )
}

@Composable
fun GenericBottomSheetHeader(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    bgColor: Color = MaterialTheme.colorScheme.surface,
) {

    Row(
        modifier = Modifier
            .background(color = bgColor)
            .padding(
                horizontal = dimensionResource(R.dimen.bottom_sheet_header_h_padding),
                vertical = dimensionResource(R.dimen.bottom_sheet_header_v_padding),
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(imageVector = icon, contentDescription = title, tint = tint)

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.item_spacer_width)))

        Column(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = title,
                color = tint,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BottomSheetHeader(
    thumb: @Composable () -> Unit,
    title: String,
    desc: String,
//    tint: Color = MaterialTheme.colorScheme.onSurface,
//    bgColor: Color = MaterialTheme.colorScheme.surface,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
//             .background(color = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.bottom_sheet_header_h_padding),
                    vertical = dimensionResource(R.dimen.bottom_sheet_header_v_padding),
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            thumb()

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.item_spacer_width)))

            Column(
                modifier = Modifier
                    .weight(1f)
//                    .padding(
//                        horizontal = dimensionResource(R.dimen.card_padding),
//                        vertical = dimensionResource(R.dimen.margin_xsmall)
//                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
//                    color = tint,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = desc,
//                    color = tint,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun BottomSheetListItem(
    icon: ImageVector?,
    title: String,
    onItemClick: () -> Unit,
    selected: Boolean = false,
) {

    // TODO Make this more generic
    val (mTint, mBg) = if (selected) {
        MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface to Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .background(color = mBg)
            .padding(
                horizontal = 16.dp,
//                 horizontal = dimensionResource(R.dimen.bottom_sheet_item_h_padding),
                // Warning: this is only the "inner" padding of the menu item.
                // See also parent's vertical spacing
                vertical = dimensionResource(R.dimen.bottom_sheet_v_padding),
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        icon?.let {
            Icon(imageVector = it, contentDescription = title, tint = mTint)
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.item_spacer_width)))
        }
        Text(text = title, color = mTint)
    }
}

@Composable
fun BottomSheetListItemWithToggle(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onItemClick: (Boolean) -> Unit
) {

    // FIXME: we want to also keep the state at this level so that the user has a direct feedback
    //  in the more menu when he launches a remote call. It does not work yet.
    val localSelected = remember {
        mutableStateOf(isSelected)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onItemClick(!isSelected) })
            .padding(
                horizontal = dimensionResource(R.dimen.bottom_sheet_item_h_padding),
//                 vertical = dimensionResource(R.dimen.bottom_sheet_v_padding),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(imageVector = icon, contentDescription = title)
        Text(
            text = title,
            modifier = Modifier.weight(1f)
        )
        Switch(
            modifier = Modifier.semantics { contentDescription = title },
            checked = isSelected,
            onCheckedChange = { localSelected.value = it; onItemClick(it) }
        )
    }
}

@Composable
fun BottomSheetDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant //.copy(alpha = .6f)
) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        color = color,
        thickness = 1.dp,
    )
}

@Preview(showBackground = true)
@Composable
fun BottomSheetContentPreview() {
    val context = LocalContext.current
    val onClick: (String) -> Unit = { title ->
        Toast.makeText(
            context, title, Toast.LENGTH_SHORT
        ).show()
    }
    val simpleMenuItems: List<SimpleMenuItem> = listOf(
        SimpleMenuItem(CellsIcons.Share, "Share", { onClick("Share") }, false),
        SimpleMenuItem(CellsIcons.Link, "Get Link", { onClick("Get Link") }, false),
        SimpleMenuItem(CellsIcons.Edit, "Edit", { onClick("Edit") }, false),
        SimpleMenuItem(CellsIcons.Delete, "Delete", { onClick("Delete") }, false),
    )

    BottomSheetContent(
        {
            BottomSheetHeader(
                icon = CellsIcons.Processing,
                title = "My Transfer of jpg.pdf",
                desc = "45MB, started at 5.54 AM, 46% done",
            )
        },
        simpleMenuItems,
    )
}

@Preview(showBackground = true)
@Composable
fun BottomSheetListItemPreview() {
    BottomSheetListItem(
        icon = CellsIcons.Processing,
        title = "Share",
        onItemClick = { },
    )
}
