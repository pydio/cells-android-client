package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.M3IconThumb
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.lists.getAppearsInDesc
import com.pydio.android.cells.ui.models.MultipleItem

@Composable
fun BookmarkListItem(
    item: MultipleItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lm = modifier.padding(0.dp, .2.dp)
    if (isSelected) {
        lm = lm.background(MaterialTheme.colorScheme.surfaceVariant)
    }
    lm = lm.padding(
        top = dimensionResource(R.dimen.list_item_inner_padding),
        bottom = dimensionResource(R.dimen.list_item_inner_padding),
        start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
        end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = lm
    ) {

        if (isSelected) {
            M3IconThumb(
                id = R.drawable.ic_baseline_check_24,
                color = MaterialTheme.colorScheme.surfaceTint
            )
        } else {
            Thumbnail(
                item.defaultStateID(),
                item.sortName,
                item.name,
                item.mime,
                item.eTag,
                -1,
                item.hasThumb
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = item.name,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = getAppearsInDesc(item),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (!isSelectionMode) {
            IconButton(onClick = { more() }) {
                Icon(
                    painter = painterResource(R.drawable.aa_300_more_vert_40px),
                    contentDescription = stringResource(R.string.open_more_menu),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.requiredSize(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }
        }
    }
}
