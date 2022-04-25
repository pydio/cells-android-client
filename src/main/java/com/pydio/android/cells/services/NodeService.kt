package com.pydio.android.cells.services

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.ROfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.nodes.TreeNodeDao
import com.pydio.android.cells.transfer.FileDownloader
import com.pydio.android.cells.transfer.ThumbDownloader
import com.pydio.android.cells.transfer.TreeDiff
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.logException
import com.pydio.android.cells.utils.parseOrder
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.Node
import com.pydio.cells.api.ui.Stats
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class NodeService(
    private val treeNodeRepository: TreeNodeRepository,
    private val accountService: AccountService,
    private val fileService: FileService,
) {

    private val logTag = NodeService::class.simpleName
    private val nodeServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + nodeServiceJob)

    fun nodeDB(stateID: StateID): TreeNodeDB {
        // TODO cache this
        val accId = treeNodeRepository.sessions[stateID.accountId]
            ?: throw IllegalStateException("No dir name found for $stateID")
        return TreeNodeDB.getDatabase(
            CellsApp.instance.applicationContext,
            stateID.accountId,
            accId.dbName,
        )
    }

    /* Expose DB content as LiveData for the ViewModels */

    fun ls(stateID: StateID): LiveData<List<RTreeNode>> {
        var encoded = CellsApp.instance.getPreference(AppNames.PREF_KEY_CURR_RECYCLER_ORDER)
        if (Str.empty(encoded)) {
            encoded = "${AppNames.SORT_BY_CANON}||${AppNames.SORT_BY_ASC}"
        }
        val orders = parseOrder(encoded!!)
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE encoded_state like '${stateID.id}%' " +
                    "AND parent_path = '${stateID.file}' " +
                    "ORDER BY ${orders.first} ${orders.second} "
        )
        return nodeDB(stateID).treeNodeDao().orderedLs(lsQuery)
    }

    fun listChildFolders(stateID: StateID): LiveData<List<RTreeNode>> {
        // Tweak to also be able to list workspaces roots
        var parPath = stateID.file
        var mime = SdkNames.NODE_MIME_FOLDER
        if (Str.empty(parPath)) {
            parPath = ""
            mime = SdkNames.NODE_MIME_WS_ROOT
        } else if (parPath == "/") {
            Log.e(
                logTag,
                "unimplemented tweak. Is it OK?  $stateID: parPath: $parPath, mime: $mime"
            )
        }
        Log.d(logTag, "Listing children of $stateID: parPath: $parPath, mime: $mime")
        return nodeDB(stateID).treeNodeDao().lsWithMime(stateID.id, parPath, mime)
    }

    fun listViewable(stateID: StateID, mimeFilter: String): LiveData<List<RTreeNode>> {
        Log.d(logTag, "Listing children of $stateID: parPath: ${stateID.file}, mime: $mimeFilter")
        return nodeDB(stateID).treeNodeDao().lsWithMimeFilter(stateID.id, stateID.file, mimeFilter)
    }

    fun listBookmarks(accountID: StateID): LiveData<List<RTreeNode>> {
        return nodeDB(accountID).treeNodeDao().getBookmarked()
    }

    fun listOfflineRoots(stateID: StateID): LiveData<List<RLiveOfflineRoot>> {
        return nodeDB(stateID).liveOfflineRootDao().getLiveOfflineRoots()
    }

    fun getLiveNode(stateID: StateID): LiveData<RTreeNode> {
        return nodeDB(stateID).treeNodeDao().getLiveNode(stateID.id)
    }

    fun getLiveNodes(stateIDs: List<StateID>): LiveData<List<RTreeNode>> {
        if (stateIDs.isEmpty()) {
            throw java.lang.IllegalStateException("Cannot retrieve live nodes without at least one ID")
        }
        val encodedIds = stateIDs.map { it.id }.toTypedArray()
        return nodeDB(stateIDs[0]).treeNodeDao().getLiveNodes(*encodedIds)
    }

    /* Communicate with the DB using suspend functions */

    /** Single entry point to insert or update a node */
    suspend fun upsertNode(newNode: RTreeNode) = withContext(Dispatchers.IO) {

        val currSession = treeNodeRepository.sessions[newNode.getStateID().accountId]
            ?: throw java.lang.IllegalStateException("No session found in cache for ${newNode.getStateID().accountId}")
        val ndb = nodeDB(newNode.getStateID())

        if (!currSession.isRemoteLegacy) {
            ndb.offlineRootDao().getByUuid(newNode.uuid)?.let {
                newNode.setOfflineRoot(true)
            }
        }

        newNode.localModificationTS = newNode.remoteModificationTS
        newNode.localModificationStatus = null
        newNode.lastCheckTS = currentTimestamp()

        val old = ndb.treeNodeDao().getNode(newNode.encodedState)
        if (old == null) {
            ndb.treeNodeDao().insert(newNode)
        } else {
            // FIXME double check that we do not loose any info
            ndb.treeNodeDao().update(newNode)
        }
    }

    suspend fun getNode(stateID: StateID): RTreeNode? = withContext(Dispatchers.IO) {
        nodeDB(stateID).treeNodeDao().getNode(stateID.id)
    }

    suspend fun queryLocally(query: String?, stateID: StateID): List<RTreeNode> =
        withContext(Dispatchers.IO) {
            return@withContext if (query == null) {
                listOf()
            } else {
                nodeDB(stateID).treeNodeDao().query(query)
            }
        }

    /* Update nodes in the local store */
    suspend fun abortLocalChanges(stateID: StateID) = withContext(Dispatchers.IO) {
        val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
        node.localModificationTS = node.remoteModificationTS
        node.localModificationStatus = null
        nodeDB(stateID).treeNodeDao().update(node)
    }

    /* Calls to query both the cache and the remote server */

    suspend fun toggleBookmark(rTreeNode: RTreeNode) = withContext(Dispatchers.IO) {
        val stateID = rTreeNode.getStateID()
        try {
            getClient(stateID).bookmark(stateID.workspace, stateID.file, !rTreeNode.isBookmarked())
            // rTreeNode.isBookmarked = !rTreeNode.isBookmarked
            // rTreeNode.localModificationTS = currentTimestamp()
            // nodeDB(stateID).treeNodeDao().update(rTreeNode)
        } catch (se: SDKException) { // Could not retrieve thumb, failing silently for the end user
            handleSdkException(stateID, "could not toggle bookmark for $stateID", se)
            return@withContext null
        } catch (ioe: IOException) {
            Log.e(logTag, "cannot toggle bookmark for ${stateID}: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext null
        }
    }

    suspend fun toggleShared(rTreeNode: RTreeNode) = withContext(Dispatchers.IO) {
        val stateID = rTreeNode.getStateID()
        try {
            val client = getClient(stateID)

            if (rTreeNode.isShared()) {
                client.unshare(stateID.workspace, stateID.file)
            } else {
                // TODO we put default values for the time being
                //   But we must handle this better
                client.share(
                    stateID.workspace, stateID.file, stateID.fileName,
                    "Created at ${Calendar.getInstance()}",
                    null, true, true
                )
            }

//             rTreeNode.isShared = !rTreeNode.isShared
//            persistUpdated(rTreeNode)
        } catch (se: SDKException) {
            Log.e(logTag, "could update share link for " + stateID.id)
            se.printStackTrace()
            return@withContext null
        } catch (ioe: IOException) {
            Log.e(logTag, "could update share link for ${stateID}: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext null
        }
    }

    suspend fun toggleOffline(rTreeNode: RTreeNode) = withContext(Dispatchers.IO) {
        val stateID = rTreeNode.getStateID()
        try {
            val db = nodeDB(stateID)
            val offlineDao = db.offlineRootDao()

            if (rTreeNode.isOfflineRoot()) {
                offlineDao.delete(rTreeNode.encodedState)
            } else {
                // TODO should we check if this node is already a descendant of
                //  an existing offline root ?
                val newRoot = ROfflineRoot.fromTreeNode(rTreeNode)
                offlineDao.insert(newRoot)
            }
            // TODO It smells
            rTreeNode.setOfflineRoot(!rTreeNode.isOfflineRoot())
            persistUpdated(rTreeNode)
        } catch (se: SDKException) {
            Log.e(logTag, "could update offline sync status for " + stateID.id)
            se.printStackTrace()
            return@withContext null
        } catch (ioe: IOException) {
            Log.e(logTag, "could update offline sync status for ${stateID}: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext null
        }
    }

    suspend fun syncAll(stateID: StateID) = withContext(Dispatchers.IO) {

        val offlineDao = nodeDB(stateID).offlineRootDao()
        val roots = offlineDao.getAll()
        val dao = nodeDB(stateID).treeNodeDao()

        for (offlineRoot in roots) {
            dao.getNode(offlineRoot.encodedState)?.let {
                // TODO handle case where remote node has disappeared on server
                launchSync(it)
            }
        }
    }

    suspend fun launchSync(rTreeNode: RTreeNode) = withContext(Dispatchers.IO) {
        val stateID = rTreeNode.getStateID()
        try {
            val client = getClient(stateID)

            val db = nodeDB(stateID)
            val treeNodeDao = nodeDB(stateID).treeNodeDao()
            val offlineDao = db.offlineRootDao()
            val currRoot = offlineDao.get(rTreeNode.encodedState)
                ?: let {
                    Log.w(logTag, "could sync $stateID, the local node has vanished")
                    return@withContext // should never happen
                }

            // TODO Rather use DI via a factory?
            val fileDL = FileDownloader()
            val thumbDL = ThumbDownloader(client, nodeDB(stateID))

            val changeNb = syncNodeAt(rTreeNode, client, treeNodeDao, fileDL, thumbDL)

            // TODO check corner cases. Typically "needs deletion"
            if (changeNb > 0) {
                currRoot.localModificationTS = currentTimestamp()
                currRoot.message = null // TODO double check
            }
            currRoot.lastCheckTS = currentTimestamp()

            offlineDao.update(currRoot)
            // TODO add more info on the corresponding root RTreeNode ??
            persistUpdated(rTreeNode)
        } catch (se: SDKException) {
            Log.e(logTag, "could update offline sync status for " + stateID.id)
            se.printStackTrace()
            return@withContext
        }
    }

//    /* Check if the current offline root need re-sync (first flag) or deletion (second flag) */
//    private suspend fun preSyncCheck(
//        offlineRoot: ROfflineRoot,
//        rTreeNode: RTreeNode,
//        client: Client,
//        treeNodeDao: TreeNodeDao,
//        offlineDao: OfflineRootDao,
//        fileDL: FileDownloader,
//        thumbDL: ThumbDownloader
//    ): Pair<Boolean, Boolean> {
//
//        // First check if everything is OK locally
//
//        return Pair(first = true, second = false)
//    }

    private suspend fun syncNodeAt(
        rTreeNode: RTreeNode,
        client: Client,
        dao: TreeNodeDao,
        fileDL: FileDownloader,
        thumbDL: ThumbDownloader
    ): Int {

        val stateID = rTreeNode.getStateID()

        // First re-sync current level
        val treeDiff = TreeDiff(stateID, client, dao, fileDL, thumbDL)
        var changeNb = treeDiff.compareWithRemote()

        if (rTreeNode.isFolder()) {
            // Then retrieve child folders and call re-sync on each one
            val children = nodeDB(stateID).treeNodeDao()
                .listWithMime(stateID.id, stateID.file, SdkNames.NODE_MIME_FOLDER)
            for (child in children) {
                changeNb += syncNodeAt(child, client, dao, fileDL, thumbDL)
            }
        }
        return changeNb
    }

    suspend fun createFolder(parentId: StateID, folderName: String) =
        withContext(Dispatchers.IO) {
            try {
                getClient(parentId).mkdir(parentId.workspace, parentId.file, folderName)
            } catch (e: SDKException) {
                val msg = "could not create folder at ${parentId.path}"
                handleSdkException(parentId, msg, e)
                return@withContext msg
            }
            return@withContext null
        }

    suspend fun copy(sources: List<StateID>, targetParent: StateID) =
        withContext(Dispatchers.IO) {
            try {
                val srcFiles = mutableListOf<String>()
                for (source in sources) {
                    srcFiles.add(source.file)
                }
                getClient(targetParent).copy(
                    targetParent.workspace,
                    srcFiles.toTypedArray(),
                    targetParent.file
                )
            } catch (e: SDKException) {
                val msg = "could not copy to $targetParent"
                handleSdkException(targetParent, msg, e)
                return@withContext msg
            }
            return@withContext null
        }

    suspend fun move(sources: List<StateID>, targetParent: StateID) =
        withContext(Dispatchers.IO) {
            try {
                val srcFiles = mutableListOf<String>()
                for (source in sources) {
                    srcFiles.add(source.file)
                }
                getClient(targetParent).move(
                    targetParent.workspace,
                    srcFiles.toTypedArray(),
                    targetParent.file
                )
            } catch (e: SDKException) {
                val msg = "could not move to $targetParent"
                handleSdkException(targetParent, msg, e)
                return@withContext msg
            }
            return@withContext null
        }

    /* Handle communication with the remote server to refresh locally stored data */

    fun enqueueDownload(stateID: StateID, uri: Uri) {
        serviceScope.launch {
            saveToSharedStorage(stateID, uri)
        }
    }

    suspend fun refreshBookmarks(stateID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            // TODO rather use a cursor than loading everything in memory...
            val nodes = mutableListOf<FileNode>()
            getClient(stateID).getBookmarks { node: Node? ->
                if (node !is FileNode) {
                    Log.w(logTag, "could not store node: $node")
                } else {
                    nodes.add(node)
                }
            }
            // Manage results
            Log.d(logTag, "Got a bookmark list of ${nodes.size} nodes, about to process")
            val dao = nodeDB(stateID).treeNodeDao()
            for (node in nodes) {
                val currNode = RTreeNode.fromFileNode(stateID, node)
                currNode.setBookmarked(true)
                val oldNode = dao.getNode(currNode.encodedState)
                if (oldNode == null) {
                    dao.insert(currNode)
                } else if (!oldNode.isBookmarked()) {
                    oldNode.setBookmarked(true)
                    dao.update(oldNode)
                }
            }
        } catch (se: SDKException) {
            val msg = "Could not refresh bookmarks from server: ${se.message}"
            handleSdkException(stateID, msg, se)
            return@withContext msg
        }
        return@withContext null
    }

    /** Retrieve the meta (and thumbs) of all readable nodes that are at the passed stateID */
    suspend fun pull(stateID: StateID): Pair<Int, String?> = withContext(Dispatchers.IO) {
        var result: Pair<Int, String?>

        try {
            val client = getClient(stateID)
            val dao = nodeDB(stateID).treeNodeDao()

            // WARNING: this browse **all** files that are in the folder
            val thumbDL = ThumbDownloader(client, nodeDB(stateID))
            val folderDiff = TreeDiff(stateID, client, dao, null, thumbDL)
            val changeNb = folderDiff.compareWithRemote()
            result = Pair(changeNb, null)
        } catch (e: SDKException) {
            val msg = "could not perform ls for ${stateID.id}, cause: ${e.message}"
            handleSdkException(stateID, msg, e)
            return@withContext Pair(0, msg)
        }
        return@withContext result
    }

    private suspend fun statRemoteNode(stateID: StateID): Stats? {
        try {
            return getClient(stateID).stats(stateID.workspace, stateID.file, true)
        } catch (e: SDKException) {
            handleSdkException(stateID, "could not stat at $stateID", e)
        }
        return null
    }

    suspend fun clearAccountCache(stateID: String): String? = withContext(Dispatchers.IO) {
        val account = StateID.fromId(StateID.fromId(stateID).accountId)
        try {
            // TODO also delete corresponding index rows
            //  Should we use 2 distinct tables for cache and offline ?

            // Delete  files:
            fileService.cleanFileCacheFor(account)
            return@withContext null
        } catch (e: Exception) {
            val msg = "Could not delete account $account"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    private suspend fun isCacheVersionUpToDate(rTreeNode: RTreeNode): Boolean? {

        if (!accountService.isClientConnected(rTreeNode.encodedState)) {
            // Cannot tell without connection
            return null
            // We admit we are happy with any local version if present
            // return rTreeNode.localFilename != null
        }

        // Compare with remote if possible
        val remoteStats = statRemoteNode(StateID.fromId(rTreeNode.encodedState)) ?: return null
        if (rTreeNode.localFileType != AppNames.LOCAL_FILE_TYPE_NONE
            && rTreeNode.localModificationTS >= remoteStats.getmTime()
        ) {
            fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_CACHE).let {
                val file = File(it)
                // TODO at this point we are not 100% sure the local file
                //  is in-line with remote, typically if update is in process
                if (file.exists()) {
                    return true
                }
            }
        }
        return false
    }

    suspend fun getOrDownloadFileToCache(rTreeNode: RTreeNode): File? =

        withContext(Dispatchers.IO) {
            Log.i(logTag, "In getOrDownloadFileToCache for ${rTreeNode.name}")
            // TODO improve check to decide whether we should download the full file or not
            val isOK = isCacheVersionUpToDate(rTreeNode)
            when {
                isOK == null && rTreeNode.localFileType != AppNames.LOCAL_FILE_TYPE_NONE
                -> fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_CACHE)
                    .let { return@withContext File(it) }
                isOK == null && rTreeNode.localFileType == AppNames.LOCAL_FILE_TYPE_NONE
                -> {
                }
                isOK ?: false
                -> return@withContext File(
                    fileService.getLocalPath(
                        rTreeNode,
                        AppNames.LOCAL_FILE_TYPE_CACHE
                    )
                )
            }

            Log.e(logTag, "... Launching download for ${rTreeNode.name}")

            val stateID = rTreeNode.getStateID()
            val baseDir =
                fileService.dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_CACHE)
            val targetFile = File(baseDir, stateID.path.substring(1))
            targetFile.parentFile!!.mkdirs()
            var out: FileOutputStream? = null

            try {
                out = FileOutputStream(targetFile)

                // TODO handle progress
                getClient(stateID).download(stateID.workspace, stateID.file, out, null)

                // Success persist change
                rTreeNode.localFileType = AppNames.LOCAL_FILE_TYPE_CACHE
                rTreeNode.localModificationTS = rTreeNode.remoteModificationTS
                // rTreeNode.localModificationTS = Calendar.getInstance().timeInMillis / 1000L
                nodeDB(stateID).treeNodeDao().update(rTreeNode)
                Log.e(logTag, "... download done for ${rTreeNode.name}")
            } catch (se: SDKException) {
                // Could not retrieve thumb, failing silently for the end user
                val msg = "could not perform DL for " + stateID.id
                handleSdkException(stateID, msg, se)
                return@withContext null
            } catch (ioe: IOException) {
                // TODO handle this: what should we do ?
                Log.e(logTag, "cannot write at ${targetFile.absolutePath}: ${ioe.message}")
                ioe.printStackTrace()
                return@withContext null
            } finally {
                IoHelpers.closeQuietly(out)
            }
            targetFile
        }

    private suspend fun saveToSharedStorage(stateID: StateID, uri: Uri) =
        withContext(Dispatchers.IO) {

            val rTreeNode =
                nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
            val resolver = CellsApp.instance.contentResolver
            var out: OutputStream? = null
            try {
                out = resolver.openOutputStream(uri)
                if (isCacheVersionUpToDate(rTreeNode) ?: return@withContext) {
                    var input: InputStream? = null
                    try {
                        input = FileInputStream(
                            getLocalFile(
                                rTreeNode,
                                AppNames.LOCAL_FILE_TYPE_CACHE
                            )
                        )
                        IoHelpers.pipeRead(input, out)
                    } finally {
                        IoHelpers.closeQuietly(input)
                    }
                    // File(getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_CACHE)).copyTo(to, true)
                } else {
                    // Directly download to final destination
                    // TODO handle progress
                    getClient(stateID).download(stateID.workspace, stateID.file, out, null)
                }
                Log.i(logTag, "... File has been copied to ${uri.path}")

            } catch (se: SDKException) { // Could not retrieve thumb, failing silently for the end user
                Log.e(logTag, "could not perform DL for " + stateID.id)
                se.printStackTrace()
            } catch (ioe: IOException) {
                // TODO handle this: what should we do ?
                Log.e(logTag, "cannot write at ${uri.path}: ${ioe.message}")
                ioe.printStackTrace()
            } finally {
                IoHelpers.closeQuietly(out)
            }
        }

    suspend fun restoreNode(stateID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not restore"
            remoteRestore(stateID)
            persistLocallyModified(node, AppNames.LOCAL_MODIF_RESTORE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not restore node: ${se.message}"
        }
        return@withContext null
    }

    suspend fun emptyRecycle(stateID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not delete"
            remoteEmptyRecycle(stateID)
            persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not empty recycle bin: ${se.message}"
        }
        return@withContext null
    }

    suspend fun rename(stateID: StateID, newName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                    ?: return@withContext "No node found at $stateID, could not rename"
                remoteRename(stateID, newName)
                persistLocallyModified(node, AppNames.LOCAL_MODIF_RENAME)
            } catch (se: SDKException) {
                se.printStackTrace()
                return@withContext "Could not delete $stateID: ${se.message}"
            }
            return@withContext null
        }

    suspend fun delete(stateID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not delete"
            remoteDelete(stateID)
            persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not delete $stateID: ${se.message}"
        }
        return@withContext null
    }

    /* Directly communicate with the distant server */
    suspend fun remoteQuery(stateID: StateID, query: String): List<RTreeNode> =
        withContext(Dispatchers.IO) {
            try {
                return@withContext getClient(stateID).search(stateID.path, query, 20)
                    .map { RTreeNode.fromFileNode(stateID, it) }
            } catch (se: SDKException) {
                se.printStackTrace()
                return@withContext listOf()
            }
        }

    private suspend fun remoteRestore(stateID: StateID): String? = withContext(Dispatchers.IO) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not restore"

            val nodes = arrayOf(node.toFileNode())
            getClient(stateID).restore(stateID.workspace, nodes)

            remoteDelete(stateID)
            persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not delete $stateID: ${se.message}"
        }
        return@withContext null
    }

    @Throws(SDKException::class)
    fun remoteEmptyRecycle(stateID: StateID) {
        getClient(stateID).emptyRecycleBin(stateID.workspace)
    }

    @Throws(SDKException::class)
    fun remoteRename(stateID: StateID, newName: String) {
        getClient(stateID).rename(stateID.workspace, stateID.file, newName)
    }

    @Throws(SDKException::class)
    fun remoteDelete(stateID: StateID) {
        getClient(stateID).delete(stateID.workspace, arrayOf<String>(stateID.file))
    }

    /* Constants and helpers */
    private fun getClient(stateId: StateID): Client {
        return accountService.getClient(stateId)
    }

    private fun persistUpdated(rTreeNode: RTreeNode) {
        rTreeNode.localModificationTS = rTreeNode.remoteModificationTS
        nodeDB(rTreeNode.getStateID()).treeNodeDao().update(rTreeNode)
    }

    private fun persistLocallyModified(rTreeNode: RTreeNode, modificationType: String) {
        rTreeNode.localModificationTS = currentTimestamp()
        rTreeNode.localModificationStatus = modificationType
        nodeDB(rTreeNode.getStateID()).treeNodeDao().update(rTreeNode)
    }

    private suspend fun handleSdkException(stateID: StateID, msg: String, se: SDKException) {
        Log.e(logTag, "Error #${se.code}: $msg")
        accountService.notifyError(stateID, se.code)
        se.printStackTrace()
    }

    // TODO Trick so that we do not store offline files also in the cache... double check
    private fun getLocalFile(item: RTreeNode, type: String): File {
        if (type == AppNames.LOCAL_FILE_TYPE_OFFLINE ||
            type == AppNames.LOCAL_FILE_TYPE_CACHE &&
            item.localFileType == AppNames.LOCAL_FILE_TYPE_OFFLINE
        ) {
            return File(fileService.getLocalPath(item, AppNames.LOCAL_FILE_TYPE_OFFLINE))
        }
        return File(fileService.getLocalPath(item, type))
    }

}
