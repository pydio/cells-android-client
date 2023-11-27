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
    nodeItem: TreeNodeItem,
    more: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    LargeCard(
        title = getNodeTitle(name = nodeItem.name, mime = nodeItem.mime),
        desc = getNodeDesc(
            nodeItem.remoteModTs,
            nodeItem.size,
            nodeItem.localModStatus
        ),
        modifier = modifier,
        isSelected = isSelected
    ) {
        if (nodeItem.hasThumb) {
            LargeCardImageThumb(
                stateID = nodeItem.defaultStateID(),
                eTag = nodeItem.eTag,
                metaHash = nodeItem.metaHash,
                title = getNodeTitle(name = nodeItem.name, mime = nodeItem.mime),
                mime = nodeItem.mime,
                openMoreMenu = more
            )
        } else {
            LargeCardGenericIconThumb(
                title = getNodeTitle(name = nodeItem.name, mime = nodeItem.mime),
                mime = nodeItem.mime,
                sortName = nodeItem.sortName,
                more = more
            )
        }
    }
}
