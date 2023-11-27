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
    isSelectionMode: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    LargeCard(
        title = item.name,
        desc = getAppearsInDesc(item),
        modifier = modifier,
        isSelected = isSelected
    ) {
        if (item.hasThumb) {
            LargeCardImageThumb(
                stateID = item.defaultStateID(),
                eTag = item.eTag,
                metaHash = item.metaHash,
                title = item.name,
                mime = item.mime,
                openMoreMenu = if (!isSelectionMode) more else null
            )
        } else {
            LargeCardGenericIconThumb(
                title = item.name,
                mime = item.mime,
                sortName = item.sortName,
                more = if (!isSelectionMode) more else null
            )
        }
    }
}

@Composable
public fun getAppearsInDesc(item: MultipleItem): String {
    val suffix = item.appearsIn
        .joinToString(", ") { item.appearsInWorkspace[it.slug] ?: it.slug }
    return stringResource(R.string.appears_in_prefix, suffix)
}