package com.pydio.android.cells.ui.models

import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID

data class TreeNodeItem(
    override val uuid: String,
    override val mime: String,
    override val name: String,
    override val eTag: String?,
    override val metaHash: Int,
    override val sortName: String?,
    override val isFolder: Boolean,
    override val hasThumb: Boolean,
    override val size: Long = -1L,
    override val remoteModTs: Long = -1L,
    override val lastCheckTS: Long = -1L,
    val stateID: StateID,
    val isWsRoot: Boolean,
    val isRecycle: Boolean,
    var isInRecycle: Boolean = false,
    val isBookmarked: Boolean,
    val isShared: Boolean,
    val isOfflineRoot: Boolean,
    val localModeTS: Long,
    val localModStatus: String?,
    var isCached: Boolean = false,
    var desc: String? = null,
) : GenericItem {

    init {
        isInRecycle = stateID.parentPath?.startsWith("/${SdkNames.RECYCLE_BIN_NAME}") == true
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TreeNodeItem) {
            return false
        }
        return (this.uuid == other.uuid)
                && (stateID == other.stateID)
                && remoteModTs == other.remoteModTs
                && localModeTS == other.localModeTS
                && metaHash == other.metaHash
                && isCached == other.isCached
                && hasThumb == other.hasThumb
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

    fun isFlag(type: Int): Boolean {
        return when (type) {
            AppNames.FLAG_BOOKMARK -> isBookmarked
            AppNames.FLAG_OFFLINE -> isOfflineRoot
            AppNames.FLAG_SHARE -> isShared
            else -> false
        }
    }

    fun showMoreMenu(connectionState: ConnectionState, isInSelectionMode: Boolean): Boolean {
        return if (isInSelectionMode) {
            false
        } else if (connectionState.serverConnection.isConnected()) {
            true
        } else {
            isFolder || isCached
        }
    }
}

suspend fun toTreeNodeItems(
    nodeService: NodeService,
    nodes: List<RTreeNode>
): List<TreeNodeItem> {
    val items: MutableList<TreeNodeItem> = mutableListOf()
    for (node in nodes) {
        val newItem = toTreeNodeItem(node, nodeService)
        items.add(newItem)
    }
    return items
}

suspend fun toTreeNodeItem(
    node: RTreeNode,
    nodeService: NodeService
): TreeNodeItem {
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
        localModeTS = node.localModificationTS,
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

    // TODO also update dirty status for this item at this point (typically mod status is too old)
    //   Also we might directly store the real File object in the item at this point
    if (node.isFolder() && node.lastCheckTS > 0) {
        newItem.isCached = true
    } else {
        nodeService.getLocalFile(node, true).first?.let {
            if (it.exists()) {
                newItem.isCached = true
            }
        }
    }
    return newItem
}
