package com.pydio.android.cells.ui.models

import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID

interface GenericItem {
    val uuid: String
    val mime: String
    val eTag: String?
    val name: String
    val sortName: String?
    val size: Long
    val remoteModTs: Long
    val isFolder: Boolean
    val hasThumb: Boolean
    fun defaultStateID(): StateID

}

data class MultipleItem(
    override val uuid: String,
    override val mime: String,
    override val eTag: String?,
    override val name: String,
    override val sortName: String?,
    override val size: Long = -1L,
    override val remoteModTs: Long = -1L,
    override val isFolder: Boolean,
    override val hasThumb: Boolean,
) : GenericItem {

    val appearsIn: MutableList<StateID> = mutableListOf()
    override fun defaultStateID(): StateID = if (appearsIn.isEmpty()) StateID.NONE else appearsIn[0]
    val appearsInWorkspace: MutableMap<String, String> = mutableMapOf()

    override fun equals(other: Any?): Boolean {
        if (other !is GenericItem) {
            return false
        }
        return this.uuid == other.uuid
    }

    override fun hashCode(): Int {
        return this.uuid.hashCode()
    }
}

suspend fun deduplicateNodes(
    nodeService: NodeService,
    nodes: List<RTreeNode>
): MutableList<MultipleItem> {
    val bis: MutableList<MultipleItem> = mutableListOf()
    // Also manage a short cache for the referenced workspace
    val wss: MutableMap<String, String> = mutableMapOf()
    for (node in nodes) {
        // We cannot rely on the fact that nodes are ordered: distinct bookmarked nodes
        // with same size or name that have "more than one path" (e.g are also in a cell)
        // might get mixed up.
        val newItem = MultipleItem(
            uuid = node.uuid,
            mime = node.mime,
            eTag = node.etag,
            name = node.name,
            sortName = node.sortName ?: node.name,
            size = node.size,
            remoteModTs = node.remoteModificationTS,
            hasThumb = node.hasThumb(),
            isFolder = node.isFolder(),
        )
        val slug = node.getStateID().slug!!
        if (!wss.containsKey(slug)) {
            nodeService.getWorkspace(node.getStateID().workspace())?.let {
                wss[slug] = it.label ?: slug
            } ?: run {
                wss[slug] = slug
            }
        }
        // We manually insure that we only reference each effective target once => might be sub-optimal for very large systems
        val existingIndex = bis.indexOf(newItem)
        if (existingIndex > -1) {
            val currItem = bis[existingIndex]
            currItem.appearsIn.add(node.getStateID())
            currItem.appearsInWorkspace[slug] = wss[slug] ?: slug
        } else {
            newItem.appearsIn.add(node.getStateID())
            newItem.appearsInWorkspace[slug] = wss[slug] ?: slug
            bis.add(newItem)
        }
    }
    return bis
}