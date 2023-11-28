package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.ServerConnection
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.models.TreeNodeItem
import com.pydio.android.cells.ui.models.toTreeNodeItem
import com.pydio.cells.transport.StateID

/**  Simply provides access to the DB to retrieve basic single objects */
class TreeNodeVM(
    private val nodeService: NodeService,
    private val preferencesService: PreferencesService,
) : ViewModel() {

    private val logTag = "TreeNodeVM"

    suspend fun getTreeNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    suspend fun getTreeNodeItem(stateID: StateID): TreeNodeItem? {
        return nodeService.getNode(stateID)?.let {
            toTreeNodeItem(it, nodeService)
        }
    }

    suspend fun mustConfirmDL(stateID: StateID, serverConnection: ServerConnection): Boolean {
        if (serverConnection == ServerConnection.OK) {
            return false
        } else if (serverConnection == ServerConnection.LIMITED) {
            nodeService.getNode(stateID)?.let {
                val limitedPrefs = preferencesService.fetchPreferences().meteredNetwork
                val fSize = it.size
                return when {
                    !limitedPrefs.applyLimits -> false
                    !limitedPrefs.askBeforeDL -> false
                    limitedPrefs.sizeThreshold <= 0 -> false
                    else -> {
                        fSize >= limitedPrefs.sizeThreshold * 1024 * 1024
                    }
                }
            }
        }
        return false
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
                metaHash = node.metaHash,
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
