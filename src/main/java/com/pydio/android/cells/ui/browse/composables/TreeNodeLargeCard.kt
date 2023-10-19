package com.pydio.android.cells.ui.browse.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.LargeCard
import com.pydio.android.cells.ui.core.composables.lists.LargeCardGenericIconThumb
import com.pydio.android.cells.ui.core.composables.lists.LargeCardImageThumb
import com.pydio.android.cells.ui.models.TreeNodeItem

@Composable
fun TreeNodeLargeCard(
    item: TreeNodeItem,
    more: () -> Unit,
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    LargeCard(
        isSelected = isSelected,
        title = getNodeTitle(name = item.name, mime = item.mime),
        desc = getNodeDesc(
            item.remoteModTs,
            item.size,
            item.localModStatus
        ),
        modifier = modifier
    ) {
        if (item.hasThumb) {
            LargeCardImageThumb(
                stateID = item.defaultStateID(),
                eTag = item.eTag,
                metaHash = item.metaHash,
                title = getNodeTitle(name = item.name, mime = item.mime),
                openMoreMenu = if (!isSelectionMode) more else null
            )
        } else {
            LargeCardGenericIconThumb(
                title = getNodeTitle(name = item.name, mime = item.mime),
                mime = item.mime,
                sortName = item.sortName,
                more = if (!isSelectionMode) more else null
            )
        }
    }
}
