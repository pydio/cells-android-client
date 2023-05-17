package com.pydio.android.cells.ui.core.composables.lists

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.models.MultipleItem

@Composable
fun MultipleGridItem(
    item: MultipleItem,
    more: () -> Unit,
    modifier: Modifier = Modifier
) {
    LargeCard(title = item.name, desc = getAppearsInDesc(item), modifier = modifier) {
        if (item.hasThumb) {
            LargeCardImageThumb(
                item.defaultStateID(),
                item.eTag,
                // FIXME
                -1,
                item.mime,
                item.name,
                item.sortName,
                more
            )
        } else {
            LargeCardIconThumb(item.name, item.mime, item.sortName, more)
        }
    }
}

@Composable
public fun getAppearsInDesc(item: MultipleItem): String {
    val suffix = item.appearsIn
        .joinToString(", ") { item.appearsInWorkspace[it.slug] ?: it.slug }
    return stringResource(R.string.appears_in_prefix, suffix)
}