package com.pydio.android.cells.transfer

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.accounts.WorkspaceDao
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TreeNodeRepository
import com.pydio.android.cells.utils.areWsNodeContentEquals
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.ui.Node
import com.pydio.cells.api.ui.WorkspaceNode
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class WorkspaceDiff(
    private val accountId: StateID,
    private val client: Client
) : KoinComponent {

    private val logTag = WorkspaceDiff::class.simpleName

    private val treeNodeRepository: TreeNodeRepository by inject()
    private val fileService: FileService by inject()
    private val wsDao: WorkspaceDao by inject()
    private val nodeDB = treeNodeRepository.nodeDB(accountId)

    private var changeNumber = 0

    suspend fun compareWithRemote() = withContext(Dispatchers.IO) {
        val remotes = RemoteWsIterator()
        remotes.listRemoteWorkspaces()
        val locals = LocalWsIterator(wsDao.getWsForDiff(accountId.id).iterator())
        processChanges(remotes, locals)
        if (changeNumber > 0) {
            Log.d(logTag, "Synced workspace list for $accountId with $changeNumber changes")
//        } else {
//            Log.e(logTag, "Synced workspace list for $accountId, no change detected")
        }

        return@withContext changeNumber
    }

    private fun processChanges(rit: Iterator<WorkspaceNode>, lit: Iterator<RWorkspace>) {

        var local = if (lit.hasNext()) lit.next() else null
        while (rit.hasNext()) {
            val remote = rit.next()
            if (local == null) {
                putAddChange(remote)
                continue
            } else {
                var order = remote.slug.compareTo(local.slug)

                while (order > 0 && lit.hasNext()) { // Next local is lexicographically smaller
                    putDeleteChange(local!!)
                    local = lit.next()
                    order = remote.slug.compareTo(local.slug)
                }
                if (order > 0) {
                    // last local is smaller than next remote, no more matches for any next remote
                    local = null
                } else if (order == 0) {
                    if (!areWsNodeContentEquals(remote, local!!)) {
                        putUpdateChange(remote)
                    }
                    // Move local cursor to next and restart the loop
                    local = if (lit.hasNext()) lit.next() else null
                    continue
                } else {
                    putAddChange(remote)
                    continue
                }
            }
        }

        // Delete remaining local workspaces that have a slug that is lexicographically greater than the last remote node
        local?.let { putDeleteChange(it) }
        while (lit.hasNext()) {
            local = lit.next()
            putDeleteChange(local)
        }
    }

    private fun putAddChange(remote: WorkspaceNode) {
        Log.d(logTag, "add for ${remote.name}")
        changeNumber++
        // We add this both on the ws and on the node table
        val rNode = RWorkspace.createChild(accountId, remote)
        wsDao.insert(rNode)

        // Also handle workspace as a RTreeNode to ease browsing and others action on cache
        val wsState = StateID.fromId(rNode.encodedState)
        val wsTreeNode = RTreeNode.fromWorkspaceNode(wsState, remote)
        nodeDB.treeNodeDao().insert(wsTreeNode)
    }

    private fun putUpdateChange(remote: WorkspaceNode) {
        Log.d(logTag, "update for ${remote.name}")
        changeNumber++
        val childStateID = accountId.child(remote.slug)

        val wsNode = RWorkspace.createChild(accountId, remote)
        wsDao.update(wsNode)

        val rNode = RTreeNode.fromWorkspaceNode(childStateID, remote)
        nodeDB.treeNodeDao().update(rNode)
    }

    private fun putDeleteChange(local: RWorkspace) {
        Log.d(logTag, "delete for ${local.slug} (${local.label})")
        changeNumber++
        deleteLocalWorkspace(local)
    }

    private fun deleteLocalWorkspace(local: RWorkspace) {

        val suffix = "/${local.slug}"

        // delete cached files
        val cacheParPath =
            fileService.dataParentPath(local.getStateID().accountId, AppNames.LOCAL_FILE_TYPE_FILE)
        val cache = File(cacheParPath + suffix)
        if (cache.exists()) {
            cache.deleteRecursively()
        }

        // delete thumbs
        val thumbParPath =
            fileService.dataParentPath(local.getStateID().accountId, AppNames.LOCAL_FILE_TYPE_THUMB)
        // FIXME
//        for (node in nodeDB.treeNodeDao().getUnder(local.encodedState)) {
//            node.thumbFilename?.let {
//                Log.i(logTag, "Got a file to delete: $it")
//                val thumb = File("${thumbParPath}/$it")
//                if (thumb.exists()) {
//                    thumb.delete()
//                }
//            }
//        }

        // remove corresponding index
        nodeDB.treeNodeDao().deleteUnder(local.encodedState)

        // delete main workspace in account DB
        wsDao.forgetWorkspace(local.encodedState)

        // TODO handle offline when implemented
    }

    // Temp wrapper to add more logs if necessary
    inner class LocalWsIterator(private val nodes: Iterator<RWorkspace>) : Iterator<RWorkspace> {
        override fun hasNext(): Boolean {
            return nodes.hasNext()
        }

        override fun next(): RWorkspace {
            return nodes.next()
        }
    }

    inner class RemoteWsIterator : Iterator<WorkspaceNode> {
        private val nodes = mutableListOf<WorkspaceNode>()
        private lateinit var nodeIterator: Iterator<WorkspaceNode>

        @Throws(SDKException::class)
        fun listRemoteWorkspaces() {
            client.workspaceList { node: Node? ->
                if (node is WorkspaceNode) {
                    nodes.add(node)
                }
            }
            nodes.sort()

//            val it = nodes.iterator()
//            while (it.hasNext()) {
//                Log.d(logTag, it.next().slug)
//            }

            nodeIterator = nodes.iterator()
        }

        override fun hasNext(): Boolean {
            return nodeIterator.hasNext()
        }

        override fun next(): WorkspaceNode {
            return nodeIterator.next()
        }
    }
}
