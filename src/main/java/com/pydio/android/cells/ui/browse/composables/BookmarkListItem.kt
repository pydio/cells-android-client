package com.pydio.android.cells.ui.browse.composables

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
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.Thumbnail
import com.pydio.android.cells.ui.core.composables.lists.getAppearsInDesc
import com.pydio.android.cells.ui.models.MultipleItem

@Composable
fun BookmarkListItem(
    item: MultipleItem,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_item_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                top = dimensionResource(R.dimen.list_item_inner_padding),
                bottom = dimensionResource(R.dimen.list_item_inner_padding),
                start = dimensionResource(R.dimen.list_item_inner_padding).times(2),
                end = dimensionResource(R.dimen.list_item_inner_padding).div(2),
            )
    ) {

        Thumbnail(
            item.defaultStateID(), item.sortName, item.name, item.mime, item.eTag, -1, item.hasThumb
        )

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
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = getAppearsInDesc(item),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

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
