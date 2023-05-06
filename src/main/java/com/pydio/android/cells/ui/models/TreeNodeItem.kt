package com.pydio.android.cells.ui.models

import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

data class TreeNodeItem(
    val stateID: StateID,
    val uuid: String,
    val mime: String,
    val name: String,
    val eTag: String?,
    val sortName: String?,
    val isWsRoot: Boolean,
    val isFolder: Boolean,
    val hasThumb: Boolean,
    val size: Long = -1L,
    val remoteModTs: Long = -1L,
    val localModStatus: String?,
    var desc: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (other !is TreeNodeItem) {
            return false
        }
        return (this.uuid == other.uuid) && (stateID == other.stateID)
    }

    override fun hashCode(): Int {
        return this.uuid.hashCode()
    }
}

suspend fun toTreeNodeItems(
    nodeService: NodeService,
    nodes: List<RTreeNode>
): List<TreeNodeItem> {
    val items: MutableList<TreeNodeItem> = mutableListOf()
    for (node in nodes) {
        val newItem = TreeNodeItem(
            stateID = node.getStateID(),
            uuid = node.uuid,
            mime = node.mime,
            eTag = node.etag,
            name = node.name,
            sortName = node.sortName ?: node.name,
            size = node.size,
            remoteModTs = node.remoteModificationTS,
            localModStatus = node.localModificationStatus,
            hasThumb = node.hasThumb(),
            isFolder = node.isFolder(),
            isWsRoot = node.isWorkspaceRoot(),
        )

        if (newItem.isWsRoot) {
            nodeService.getWorkspace(node.getStateID().workspace())?.let {
                newItem.desc = it.description
            }
        }
        items.add(newItem)
    }
    return items
}
