package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.cells.transport.StateID

/**  Simply provides access to the DB to retrieve basic single objects */
class TreeNodeVM(
    private val nodeService: NodeService,
) : ViewModel() {

    private val logTag = "TreeNodeVM"

    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    suspend fun getWS(stateID: StateID): RWorkspace? {
        return nodeService.getWorkspace(stateID)
    }

    suspend fun appearsIn(stateID: StateID): MultipleItem? {
        getTreeNode(stateID)?.let { node ->
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
            nodeService.getNodesByUuid(stateID, node.uuid).forEach { curr ->
                val slug = curr.getStateID().slug!!
                newItem.appearsIn.add(curr.getStateID())
                newItem.appearsInWorkspace[slug] =
                    nodeService.getWorkspace(curr.getStateID().workspace())?.let {
                        it.label ?: slug
                    } ?: run {
                        slug
                    }
            }
            return newItem
        }
        return null
    }

    init {
        Log.i(logTag, "Created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(logTag, "Cleared")
    }
}
