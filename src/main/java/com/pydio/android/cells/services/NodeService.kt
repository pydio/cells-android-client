package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.bumptech.glide.Glide
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.transfer.TreeDiff
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.currentTimestampAsString
import com.pydio.android.cells.utils.logException
import com.pydio.android.cells.utils.parseOrder
import com.pydio.cells.api.Client
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.Node
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class NodeService(
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val offlineService: OfflineService,
    private val fileService: FileService,
) {
    private val logTag = "NodeService"
    private val nodeServiceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(ioDispatcher + nodeServiceJob)

    // Query the local index to get LiveData for the ViewModels
    fun sortedList(stateID: StateID, encodedSortBy: String): LiveData<List<RTreeNode>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedSortBy, ListType.DEFAULT)
        val parPath = stateID.file
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE encoded_state like '${stateID.id}%' " +
                    "AND parent_path = ? " +
                    "ORDER BY $sortByCol $sortByOrder ", arrayOf(parPath)
        )
        return nodeDB(stateID).treeNodeDao().treeNodeQuery(lsQuery)
    }

    fun listBookmarks(
        accountID: StateID,
        sortByCol: String,
        sortByOrder: String
    ): LiveData<List<RTreeNode>> {
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE flags & " + AppNames.FLAG_BOOKMARK +
                    " = " + AppNames.FLAG_BOOKMARK + " ORDER BY $sortByCol $sortByOrder"
        )
        return nodeDB(accountID).treeNodeDao().treeNodeQuery(lsQuery)
    }

    fun listOfflineRoots(
        accountID: StateID,
        encodedOrder: String,
    ): LiveData<List<RLiveOfflineRoot>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedOrder, ListType.DEFAULT)
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM RLiveOfflineRoot WHERE " +
                    "status != '${AppNames.OFFLINE_STATUS_LOST}' ORDER BY $sortByCol $sortByOrder"
        )
        return nodeDB(accountID).liveOfflineRootDao().offlineRootQuery(lsQuery)
    }

    fun listWorkspaces(stateID: StateID): LiveData<List<RTreeNode>> {
        return nodeDB(stateID).treeNodeDao().lsWithMime(stateID.id, "", SdkNames.NODE_MIME_WS_ROOT)
    }

    fun listViewable(stateID: StateID, mimeFilter: String): LiveData<List<RTreeNode>> {
        Log.d(logTag, "Listing children of $stateID: parPath: ${stateID.file}, mime: $mimeFilter")
        return nodeDB(stateID).treeNodeDao().lsWithMimeFilter(stateID.id, stateID.file, mimeFilter)
    }

    fun liveSearch(
        stateID: StateID,
        query: String,
        encodedSortBy: String
    ): LiveData<List<RTreeNode>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedSortBy, ListType.DEFAULT)
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE name like '%${query}%' " +
                    "ORDER BY $sortByCol $sortByOrder LIMIT 100 "
        )
        return nodeDB(stateID).treeNodeDao().liveSearchQuery(lsQuery)
    }

    /* Communicate with the DB using suspend functions */
    suspend fun getNode(stateID: StateID): RTreeNode? = withContext(ioDispatcher) {
        if (stateID == StateID.NONE) {
            null
        } else {
            nodeDB(stateID).treeNodeDao().getNode(stateID.id)
        }
    }

    suspend fun searchLocally(
        stateID: StateID,
        query: String,
        encodedSortBy: String
    ): List<RTreeNode> = withContext(ioDispatcher) {
        if (Str.empty(query)) {
            listOf()
        } else {
            val (sortByCol, sortByOrder) = parseOrder(encodedSortBy, ListType.DEFAULT)
            val lsQuery = SimpleSQLiteQuery(
                "SELECT * FROM tree_nodes WHERE name like '%${query}%' " +
                        "ORDER BY $sortByCol $sortByOrder LIMIT 100 "
            )
            nodeDB(stateID).treeNodeDao().searchQuery(lsQuery)
        }
    }

    suspend fun getWorkspace(stateID: StateID): RWorkspace? {
        return if (stateID == StateID.NONE) {
            null
        } else {
            accountService.getWorkspace(stateID)
        }
    }

    /** Single entry point to insert or update a node */
    suspend fun upsertNode(newNode: RTreeNode, isDiffRoot: Boolean = false) =
        withContext(ioDispatcher) {

            val state = newNode.getStateID()
            val currSession = treeNodeRepository.sessions[newNode.getStateID().accountId]
                ?: throw java.lang.IllegalStateException("No session found in cache for ${newNode.getStateID().accountId}")
            val ndb = nodeDB(state)

            // Also cache offline status and public link URL locally
            if (!currSession.isRemoteLegacy) {
                ndb.offlineRootDao().getByUuid(newNode.uuid)?.let {
                    if (it.encodedState != newNode.encodedState) {
                        // TODO we should rather try to move existing offline root
                        offlineService.removeOfflineRoot(state)
                        offlineService.updateOfflineRoot(newNode)
                    } else {
                        newNode.setOfflineRoot(true)
                    }
                }
            }
            var address: String? = null
            val isShared =
                newNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARED, "false") == "true"
            if (isShared) {
                val client = accountService.getClient(state)
                if (client.isLegacy) {
                    address = client.getShareAddress(state.workspace, state.file)
                } else {
                    newNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                        address = client.getShareAddress(state.workspace, it)
                    }
                }
            }
            newNode.setShared(isShared, address)

            newNode.localModificationTS = newNode.remoteModificationTS
            newNode.localModificationStatus = null

            if (newNode.isFile() || isDiffRoot) {
                // We only update last check TS on folder if explicitly required by param,
                // after checking the full content of a folder, in order to differentiate
                // not-yet loaded folders from empty ones.
                newNode.lastCheckTS = currentTimestamp()
            }

            val old = ndb.treeNodeDao().getNode(newNode.encodedState)
            Log.d(logTag, "upserting node at: ${old?.getStateID() ?: state}")

            if (old == null) {
                ndb.treeNodeDao().insert(newNode)
            } else {
                // TODO double check that we do not loose any info
                //    (not true with RLocalFile object anymore) -> Typically we force re-download of thumbs at each update
                ndb.treeNodeDao().update(newNode)
            }
        }

    /* Update nodes in the local store */

    /* Calls to query both the cache and the remote server */

    suspend fun toggleBookmark(stateID: StateID, newState: Boolean) = withContext(ioDispatcher) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
            getClient(stateID).bookmark(stateID.workspace, stateID.file, newState)
            node.setBookmarked(newState)
            node.localModificationTS = currentTimestamp()
            nodeDB(stateID).treeNodeDao().update(node)
        } catch (se: SDKException) { // Could not retrieve thumb, failing silently for the end user
            handleSdkException(stateID, "could not toggle bookmark for $stateID", se)
            return@withContext
        } catch (ioe: IOException) {
            Log.e(logTag, "cannot toggle bookmark for ${stateID}: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext
        }
    }

    suspend fun removeShare(stateID: StateID) = withContext(ioDispatcher) {
        try {
            val client = getClient(stateID)
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
            if (client.isLegacy) {
                client.unshare(stateID.workspace, stateID.file)
            } else {
                node.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                    client.unshare(stateID.workspace, it)
                }
            }
        } catch (se: SDKException) {
            Log.e(logTag, "could remove link for " + stateID.id)
        } catch (ioe: IOException) {
            Log.e(logTag, "could remove link for ${stateID}: ${ioe.message}")
            return@withContext
        }
    }

    @Throws(SDKException::class)
    suspend fun createShare(stateID: StateID): String? = withContext(ioDispatcher) {
        try {
            // We still put default values. TODO implement user defined details
            return@withContext getClient(stateID).share(
                stateID.workspace, stateID.file, stateID.fileName,
                "Created on ${currentTimestampAsString()}",
                null, true, true
            )
        } catch (se: SDKException) {
            throw SDKException(se.code, "could create link for $stateID", se)
        } catch (ioe: IOException) {
            throw SDKException(ErrorCodes.internal_error, "could create link for $stateID", ioe)
        }
    }

    @Deprecated("Rather use createShare() and removeShare()")
    suspend fun toggleShared(rTreeNode: RTreeNode): String? = withContext(ioDispatcher) {
        val stateID = rTreeNode.getStateID()
        try {
            val client = getClient(stateID)
            if (rTreeNode.isShared()) {
                if (client.isLegacy) {
                    client.unshare(stateID.workspace, stateID.file)
                } else {
                    rTreeNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                        client.unshare(stateID.workspace, it)
                    }
                }
            } else {
                // We still put default values. TODO implement user defined details
                return@withContext client.share(
                    stateID.workspace, stateID.file, stateID.fileName,
                    "Created on ${currentTimestampAsString()}",
                    null, true, true
                )
            }
        } catch (se: SDKException) {
            Log.e(logTag, "could update share link for " + stateID.id)
            se.printStackTrace()
            return@withContext null
        } catch (ioe: IOException) {
            Log.e(logTag, "could update share link for ${stateID}: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext null
        }
        return@withContext null
    }

    suspend fun createFolder(parentID: StateID, folderName: String) =
        withContext(ioDispatcher) {
            try {
                getClient(parentID).mkdir(parentID.workspace, parentID.file, folderName)
            } catch (e: SDKException) {
                val msg = "could not create folder at ${parentID.path}"
                handleSdkException(parentID, msg, e)
                return@withContext msg
            }
            return@withContext null
        }

    suspend fun copy(sources: List<StateID>, targetParent: StateID) =
        withContext(ioDispatcher) {
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
        withContext(ioDispatcher) {
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


    // Handle communication with the remote server to refresh locally stored data.

    /**
     * Retrieve the meta of all readable nodes that are at the passed stateID.
     * Files and thumbs are lazily retrieved by Glide (for images) or upon user request (for all
     * other files).
     */
    suspend fun pull(stateID: StateID): Pair<Int, String?> = withContext(ioDispatcher) {
        try {
            val client = getClient(stateID)
            val dao = nodeDB(stateID).treeNodeDao()

            // WARNING: this browse **all** files that are in the folder
            val folderDiff = TreeDiff(stateID, client, dao, null)
            val changeNb = folderDiff.compareWithRemote()
            return@withContext Pair(changeNb, null)
        } catch (e: SDKException) {
            val msg = "could not perform ls for $stateID"
            handleSdkException(stateID, msg, e)
            return@withContext Pair(0, msg)
        }
    }

    @Throws(SDKException::class)
    suspend fun tryToCacheNode(stateID: StateID): RTreeNode? = withContext(ioDispatcher) {
        try {
            val fileNode = getClient(stateID).nodeInfo(stateID.workspace, stateID.file)
            val treeNode = RTreeNode.fromFileNode(stateID, fileNode)
            upsertNode(treeNode)
            return@withContext treeNode
        } catch (e: SDKException) {
            val msg = "could not cache node at $stateID"
            Log.w(logTag, "$msg, cause: ${e.message ?: "NaN"}")
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun refreshBookmarks(stateID: StateID) = withContext(ioDispatcher) {
        try {
            val dao = nodeDB(stateID).treeNodeDao()
            val nodes = mutableListOf<FileNode>()
            getClient(stateID).getBookmarks { node: Node? ->
                if (node !is FileNode) {
                    Log.w(logTag, "could not store node: $node")
                } else {
                    nodes.add(node)
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
            }
        } catch (se: SDKException) {
            val msg = "Could not refresh bookmarks from server: ${se.message}"
            handleSdkException(stateID, msg, se)
            throw SDKException(ErrorCodes.api_error, "Could not refresh bookmarks for $stateID", se)
        }
    }

    private suspend fun getNodeInfo(stateID: StateID): FileNode? {
        try {
            return getClient(stateID).nodeInfo(stateID.workspace, stateID.file)
        } catch (e: SDKException) {
            handleSdkException(stateID, "could not getNodeInfo for $stateID", e)
            throw e
        }
    }

    @Deprecated("Rather use the method with the StateID")
    suspend fun clearAccountCache(stateId: String): String? = withContext(ioDispatcher) {
        return@withContext clearAccountCache(StateID.fromId(stateId).account())
    }

    suspend fun clearAccountCache(accountID: StateID): String? = withContext(ioDispatcher) {
        // val accountID = StateID.fromId(stateID).account()
        try {
            // First delete corresponding files
            fileService.cleanFileCacheFor(accountID)

            // Also clean index
            val offlineDao = nodeDB(accountID).offlineRootDao()
            val offlinePaths = offlineDao.getAllActive().map { it.encodedState }
            val treeNodeDao = nodeDB(accountID).treeNodeDao()
            for (record in treeNodeDao.getUnder(accountID.id)) {
                if (!isInOfflineTree(offlinePaths, record.encodedState)) {
                    treeNodeDao.delete(record.encodedState)
                }
            }

            // TODO finalise cache cleaning for glide
            // Yet, this should violently and completely empty Glide's cache

            // Must be called on the main thread
            withContext(Dispatchers.Main) {
                Glide.get(appContext).clearMemory()
            }
            // Must be called on a background thread.
            Glide.get(appContext).clearDiskCache()

            return@withContext null

        } catch (e: Exception) {
            val msg = "Could not clear cache for $accountID"
            logException(logTag, msg, e)
            return@withContext msg
        }
    }

    private fun isInOfflineTree(rootPaths: List<String>, currentPath: String): Boolean {
        return rootPaths.any { currentPath.startsWith(it) }
    }

    suspend fun isCachedVersionUpToDate(rTreeNode: RTreeNode): Boolean? {

        val localFileDao = treeNodeRepository.nodeDB(rTreeNode.getStateID()).localFileDao()
        val localFile = localFileDao.getFile(rTreeNode.encodedState, AppNames.LOCAL_FILE_TYPE_FILE)

        // Corner case: no internet connection
        if (!accountService.isClientConnected(rTreeNode.getStateID())) {
            // We admit we are happy with any local version that is found
            localFile?.let { return true }
            // File is not there and we have no connection returning null to let calling class handle this
                ?: let { return null }
        }

        localFile ?: return false

        // Compare with remote if possible
        val remoteFileNode = getNodeInfo(rTreeNode.getStateID()) ?: throw SDKException(
            ErrorCodes.not_found,
            "No node found on remote server for ${rTreeNode.getStateID()}"
        )
        val nodeInfo = RTreeNode.fromFileNode(rTreeNode.getStateID(), remoteFileNode)
        return fileService.isLocalFileUpToDate(nodeInfo, localFile)
    }

    /**
     * Returns the local file to be opened if it exists, optionally after checking
     * if it is still up to date based on:
     * - modification time
     * - e-tag
     * - size
     */
    suspend fun getLocalFile(rTreeNode: RTreeNode, checkUpToDate: Boolean): File? =
        withContext(ioDispatcher) {
            Log.d(
                logTag,
                "Getting LocalFile for [${rTreeNode.getStateID()}], check: $checkUpToDate"
            )
            val file = File(fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_FILE))
            if (!file.exists()) {
                Log.e(logTag, "File not found at ${file.absolutePath}")
                return@withContext null
            }

            if (!checkUpToDate) {
                return@withContext file
            }
            try {
                // Compare with remote if possible
                val remote = getNodeInfo(rTreeNode.getStateID())
                // We cannot stat remote, but we have a file let's open this one
                    ?: run {
                        Log.e(logTag, "No info found")
                        return@withContext file
                    }

                val isUpToDate = rTreeNode.etag == remote.eTag &&
                        rTreeNode.size == remote.size &&
                        rTreeNode.remoteModificationTS >= remote.lastModified

                return@withContext if (isUpToDate) file else null
            } catch (se: SDKException) {
                // Could not retrieve node info to compare. cf above
                val msg = "could not stat ${rTreeNode.getStateID()}"
                handleSdkException(rTreeNode.getStateID(), msg, se)
                return@withContext null
            }
        }

    suspend fun restoreNode(stateID: StateID): String? = withContext(ioDispatcher) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not restore"
            remoteRestore(stateID)
            treeNodeRepository.persistLocallyModified(node, AppNames.LOCAL_MODIF_RESTORE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not restore node: ${se.message}"
        }
        return@withContext null
    }

    suspend fun rename(stateID: StateID, newName: String): String? =
        withContext(ioDispatcher) {
            try {
                val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                    ?: return@withContext "No node found at $stateID, could not rename"
                remoteRename(stateID, newName)
                treeNodeRepository.persistLocallyModified(node, AppNames.LOCAL_MODIF_RENAME)
            } catch (se: SDKException) {
                se.printStackTrace()
                return@withContext "Could not rename $stateID: ${se.message}"
            }
            return@withContext null
        }

    suspend fun delete(stateID: StateID): String? = withContext(ioDispatcher) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not delete"
            remoteDelete(stateID)
            treeNodeRepository.persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not delete $stateID: ${se.message}"
        }
        return@withContext null
    }

    /* Directly communicate with the distant server */
    suspend fun remoteQuery(stateID: StateID, query: String): List<RTreeNode> =
        withContext(ioDispatcher) {
            try {
                val nodes = getClient(stateID).search(stateID.path ?: "/", query, 20)
                    .map {
//                        Log.e(logTag, "mapping query result for $stateID: ${it.path}")
                        RTreeNode.fromFileNode(stateID, it)
                    }

                // We already insert found nodes in the cache to ease following user action
                for (node in nodes) {
                    // Log.e(logTag, "handling query result for ${node.getStateID()} ")
                    getNode(node.getStateID())?.let {
                        // Log.e(logTag, "found ${it.getStateID()}, doing nothing")
                    } ?: upsertNode(node)
                }

                return@withContext nodes
            } catch (se: SDKException) {
                se.printStackTrace()
                return@withContext listOf()
            }
        }

    private suspend fun remoteRestore(stateID: StateID): String? = withContext(ioDispatcher) {
        try {
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext "No node found at $stateID, could not restore"

            val nodes = arrayOf(node.toFileNode())
            getClient(stateID).restore(stateID.workspace, nodes)

            remoteDelete(stateID)
            treeNodeRepository.persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not restore $stateID: ${se.message}"
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
    private suspend fun handleSdkException(stateID: StateID, msg: String, se: SDKException) {
        Log.e(logTag, "Error #${se.code}: $msg")
        // se.printStackTrace()
        accountService.notifyError(stateID, msg, se)
    }

    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

    private fun getClient(stateID: StateID): Client {
        return accountService.getClient(stateID)
    }

    fun getLocalFile(item: RTreeNode, type: String): File {
        return File(fileService.getLocalPath(item, type))
    }
}
