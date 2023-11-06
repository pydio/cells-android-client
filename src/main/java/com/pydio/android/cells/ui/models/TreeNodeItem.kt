package com.pydio.android.cells.ui.models

import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

data class TreeNodeItem(
    val stateID: StateID,
    override val uuid: String,
    override val mime: String,
    override val name: String,
    override val eTag: String?,
    override val metaHash: Int,
    override val sortName: String?,
    val isWsRoot: Boolean,
    override val isFolder: Boolean,
    val isBookmarked: Boolean,
    val isShared: Boolean,
    val isOfflineRoot: Boolean,
    val isRecycle: Boolean = false,
    override val hasThumb: Boolean,
    override val size: Long = -1L,
    override val remoteModTs: Long = -1L,
    override val lastCheckTS: Long = -1L,
    val localModStatus: String?,
    var desc: String? = null,
) : GenericItem {

    override fun equals(other: Any?): Boolean {
        if (other !is TreeNodeItem) {
            return false
        }
        return (this.uuid == other.uuid)
                && (stateID == other.stateID)
                && remoteModTs == other.remoteModTs
                && metaHash == other.metaHash
                && ((eTag ?: "") == (other.eTag ?: ""))
                && ((localModStatus ?: "") == (other.localModStatus ?: ""))
                && lastCheckTS == other.lastCheckTS
    }

    override fun hashCode(): Int {
        return this.uuid.hashCode()
    }

    override fun defaultStateID(): StateID {
        return stateID
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
            metaHash = node.metaHash,
            name = node.name,
            sortName = node.sortName ?: node.name,
            size = node.size,
            remoteModTs = node.remoteModificationTS,
            lastCheckTS = node.lastCheckTS,
            localModStatus = node.localModificationStatus,
            hasThumb = node.hasThumb(),
            isFolder = node.isFolder(),
            isRecycle = node.isRecycle(),
            isWsRoot = node.isWorkspaceRoot(),
            isBookmarked = node.isBookmarked(),
            isOfflineRoot = node.isOfflineRoot(),
            isShared = node.isShared()
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
