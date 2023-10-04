package com.pydio.android.cells.ui.browse.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pydio.android.cells.ui.core.composables.getNodeDesc
import com.pydio.android.cells.ui.core.composables.getNodeTitle
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithIcon
import com.pydio.android.cells.ui.core.composables.lists.LargeCardWithImage
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.cells.transport.StateID

@Composable
fun TreeNodeLargeCard(
    node: TreeNodeItem,
    openMoreMenu: (StateID) -> Unit,
    open: (StateID) -> Unit
) {
    if (node.hasThumb) {
        LargeCardWithImage(
            stateID = node.stateID,
            eTag = node.eTag,
            metaHash = node.metaHash,
            mime = node.mime,
            title = getNodeTitle(name = node.name, mime = node.mime),
            desc = getNodeDesc(
                node.remoteModTs,
                node.size,
                node.localModStatus
            ),
            openMoreMenu = { openMoreMenu(node.stateID) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open(node.stateID) }
        )
    } else {
        LargeCardWithIcon(
            sortName = node.sortName,
            mime = node.mime,
            title = getNodeTitle(name = node.name, mime = node.mime),
            desc = getNodeDesc(
                node.remoteModTs,
                node.size,
                node.localModStatus
            ),
            openMoreMenu = { openMoreMenu(node.stateID) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open(node.stateID) }
        )
    }
}
