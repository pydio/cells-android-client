package com.pydio.android.cells.transfer

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDao
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.areNodeContentEquals
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.PageOptions
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class TreeDiff(
    private val baseFolderStateId: StateID,
    private val client: Client,
    private val dao: TreeNodeDao,
    private val fileDL: FileDownloader?,
) : KoinComponent {

    companion object {
        private const val PAGE_SIZE = 100
        fun firstPage(): PageOptions {
            val page = PageOptions()
            page.limit = PAGE_SIZE
            page.offset = 0
            page.currentPage = 0
            page.total = -1
            page.totalPages = -1
            return page
        }
    }

    private val logTag = TreeDiff::class.java.simpleName
    private val folderDiffJob = Job()
    private val diffScope = CoroutineScope(Dispatchers.IO + folderDiffJob)

    private val nodeService: NodeService by inject()
    private val fileService: FileService by inject()

//    private val thumbParPath =
//        fileService.dataParentPath(baseFolderStateId.accountId, AppNames.LOCAL_FILE_TYPE_THUMB)
//    private val previewParPath =
//        fileService.dataParentPath(baseFolderStateId.accountId, AppNames.LOCAL_FILE_TYPE_PREVIEW)

    private var alsoCheckFiles = fileDL != null

    private var changeNumber = 0

    /** Retrieve the meta of all readable nodes that are at the passed stateID */
    @Throws(SDKException::class)
    suspend fun compareWithRemote() = withContext(Dispatchers.IO) {
        Log.d(logTag, "Launching diff for $baseFolderStateId")

        // First insure node has not been erased on the server since last visit
        val local = dao.getNode(baseFolderStateId.id)
        var remote: FileNode? = null
        try {
            remote = client.nodeInfo(baseFolderStateId.workspace, baseFolderStateId.file)
        } catch (e: SDKException) {
            val msg = "stat failed at ${baseFolderStateId}: ${e.message}"
            Log.e(logTag, msg)
            // Corner case: connection failed, we just return with no change
            if (e.isConnectionFailedError) {
                throw e
            }
        }

        if (remote == null) {
            local?.let {
                putDeleteChange(it)
                return@withContext 1
            }
            return@withContext 0
        }

        // Then perform real diff
        if (remote.isFolder) {
            handleFolder()
        } else {
            when {
                local == null -> {
                    putAddChange(remote)
                }
                areNodeContentEquals(remote, local, client.isLegacy) -> {
                    if (alsoCheckFiles) {
                        checkFiles(local.getStateID(), remote)
                    }
                }
                else -> {
                    putUpdateChange(remote, local)
                }
            }
        }

        if (changeNumber > 0) {
            Log.d(logTag, "Synced node at $baseFolderStateId with $changeNumber changes")
        }

        return@withContext changeNumber
    }

    private suspend fun handleFolder() {
        val remotes = RemoteNodeIterator(baseFolderStateId)
        val locals = dao.getNodesForDiff(baseFolderStateId.id, baseFolderStateId.file).iterator()
        processChanges(remotes, locals)

        // Update info for current folder
        if (baseFolderStateId.file == "/") {
            nodeService.getNode(baseFolderStateId)?.let {
                it.lastCheckTS = currentTimestamp()
                dao.update(it)
            }
        } else if (baseFolderStateId.file != null) {
            client.nodeInfo(baseFolderStateId.workspace, baseFolderStateId.file)?.let { parFolder ->
                nodeService.upsertNode(RTreeNode.fromFileNode(baseFolderStateId, parFolder), true)
            }
        }
    }

    private suspend fun processChanges(rit: Iterator<FileNode>, lit: Iterator<RTreeNode>) {

        var local = if (lit.hasNext()) lit.next() else null
        while (rit.hasNext()) {
            val remote = rit.next()
            if (local == null) {
                putAddChange(remote)
                continue
            } else {
                var order = remote.name.compareTo(local.name)

                while (order > 0 && lit.hasNext()) { // Next local is lexicographically smaller
                    putDeleteChange(local!!)
                    local = lit.next()
                    order = remote.name.compareTo(local.name)
                }
                if (order > 0) {
                    // last local is smaller than next remote, no more matches for any next remote
                    local = null
                } else if (order == 0) {
                    if (areNodeContentEquals(remote, local!!, client.isLegacy)) {
                        checkFiles(local.getStateID(), remote)
                    } else {
                        putUpdateChange(remote, local)
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

        // Delete remaining local nodes that have name greater than the last remote node
        local?.let { putDeleteChange(it) }
        while (lit.hasNext()) {
            local = lit.next()
            putDeleteChange(local)
        }
    }

    private suspend fun putAddChange(remote: FileNode) {
        Log.d(logTag, "add for ${remote.name}")
        changeNumber++
        val childStateID = baseFolderStateId.child(remote.name)
        val rNode = RTreeNode.fromFileNode(childStateID, remote)
        nodeService.upsertNode(rNode)
        checkFiles(childStateID, remote)
    }

    private suspend fun putUpdateChange(remote: FileNode, local: RTreeNode) {
        Log.d(logTag, "update for ${remote.name}")

        changeNumber++

        // TODO: Insure corner cases are correctly handled, typically on type switch
        val childStateID = baseFolderStateId.child(remote.name)
        val rNode = RTreeNode.fromFileNode(childStateID, remote)
        if (local.isFolder() && remote.isFile) {
            deleteLocalFolder(local)
        }
        nodeService.upsertNode(rNode)
        checkFiles(childStateID, remote)
    }

    private fun putDeleteChange(local: RTreeNode) {
        Log.d(logTag, "delete for ${local.name}")
        changeNumber++
        when {
            local.isFolder() -> deleteLocalFolder(local)
            else -> deleteLocalFile(local)
        }
    }

    /* LOCAL HELPERS */
    private fun checkFiles(stateID: StateID, remote: FileNode) {

        if (!alsoCheckFiles || fileDL == null) {
            return
        }

        if (remote.hasThumb() &&
            fileService.needsUpdate(stateID, remote, AppNames.LOCAL_FILE_TYPE_THUMB)
        ) {
            diffScope.launch {
                fileDL.orderDL(stateID.id, AppNames.LOCAL_FILE_TYPE_THUMB)
            }
        }

        if (remote.isPreViewable &&
            fileService.needsUpdate(stateID, remote, AppNames.LOCAL_FILE_TYPE_PREVIEW)
        ) {
            diffScope.launch {
                fileDL.orderDL(stateID.id, AppNames.LOCAL_FILE_TYPE_PREVIEW)
            }
        }

        if (remote.isFile &&
            fileDL != null &&
            fileService.needsUpdate(stateID, remote, AppNames.LOCAL_FILE_TYPE_FILE)
        ) {
            diffScope.launch {
                fileDL.orderDL(stateID.id, AppNames.LOCAL_FILE_TYPE_FILE)
            }
        }
    }

    private fun deleteLocalFile(local: RTreeNode) {
        // Local thumbs and cached files
        fileService.deleteCachedFilesFor(local)
        // Remove from index.
        dao.delete(local.encodedState)
    }

    private fun deleteLocalFolder(local: RTreeNode) {
        // Local file deletion, we must use the index and delete them one by one
        // because thumb like files are all in a single bucket
        fileService.deleteCachedFileRecursively(local.getStateID())
        // Also remove folders in the tree structure
        val file = File(fileService.getLocalPath(local, AppNames.LOCAL_FILE_TYPE_FILE))
        if (file.exists()) {
            file.deleteRecursively()
        }
        // Remove current folder and children in the index
        dao.deleteUnder(local.encodedState)
        dao.delete(local.encodedState)
    }

    /**
     * Convenience class that iterates on remote pages to list the full content of
     * a remote workspace or folder
     */
    inner class RemoteNodeIterator(private val parentId: StateID) : Iterator<FileNode> {

        private val nodes = mutableListOf<FileNode>()

        private var nodeIterator = nodes.iterator()
        private var nextPage = firstPage()

        override fun hasNext(): Boolean {
            if (nodeIterator.hasNext()) {
                return true
            }

            if (nextPage.currentPage != nextPage.totalPages) {
                getNextPage(nextPage)
                nodeIterator = nodes.iterator()
            }

            return nodeIterator.hasNext()
        }

        override fun next(): FileNode {
            return nodeIterator.next()
        }

        private fun getNextPage(page: PageOptions) {
            nodes.clear()

            if (client.isLegacy) {
                getAllSorted()
            } else {
                nextPage = client.ls(parentId.workspace, parentId.file, page) {
                    if (it !is FileNode) {
                        Log.w(logTag, "could not store node: $it")
                    } else {
                        nodes.add(it)
                    }
                }
            }
        }

        // P8 specific, we must retrieve all nodes at this point and sort them for our
        // diff algorithm to work
        private fun getAllSorted() {
            val unsorted = mutableListOf<FileNode>()
            while (nextPage.currentPage != nextPage.totalPages) {
                nextPage = client.ls(parentId.workspace, parentId.file, nextPage) {
                    if (it !is FileNode) {
                        Log.w(logTag, "could not store node: $it")
                    } else {
                        unsorted.add(it)
                    }
                }
                nodes.addAll(unsorted.sorted())
                nodeIterator = nodes.iterator()
            }
        }
    }

// Temp wrapper to add more logs
//    inner class LocalNodeIterator(private val nodes: Iterator<RTreeNode>) : Iterator<RTreeNode> {
//        override fun hasNext(): Boolean {
//            return nodes.hasNext()
//        }
//
//        override fun next(): RTreeNode {
//            val next = nodes.next()
//            Log.i(TAG, "Local: ${next.name}")
//            return next
//        }
//    }
}
