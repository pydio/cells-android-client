package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class NodeService(
    private val appContext: Context,
    coroutineService: CoroutineService,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val offlineService: OfflineService,
    private val fileService: FileService,
) {
    private val logTag = "NodeService"

    private val serviceScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    // Query the local index to get Flows for the ViewModels

    fun sortedListFlow(
        stateID: StateID,
        sortByCol: String,
        sortByOrder: String
    ): Flow<List<RTreeNode>> {
        val parPath = stateID.file
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE encoded_state like '${stateID.id}%' " +
                    "AND parent_path = ? " +
                    "ORDER BY $sortByCol $sortByOrder ", arrayOf(parPath)
        )
        return nodeDB(stateID).treeNodeDao().searchQueryFlow(lsQuery)
    }

    fun listBookmarkFlow(
        accountID: StateID,
        sortByCol: String,
        sortByOrder: String
    ): Flow<List<RTreeNode>> {
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE flags & " + AppNames.FLAG_BOOKMARK +
                    " = " + AppNames.FLAG_BOOKMARK + " ORDER BY $sortByCol $sortByOrder"
        )
        return nodeDB(accountID).treeNodeDao().searchQueryFlow(lsQuery)
    }

    fun listWorkspaces(stateID: StateID): Flow<List<RTreeNode>> {
        return nodeDB(stateID).treeNodeDao()
            .lsWithMimeFlow(stateID.id, "", SdkNames.NODE_MIME_WS_ROOT)
    }

    fun listChildren(stateID: StateID, mimeFilter: String): Flow<List<RTreeNode>> {
        Log.d(logTag, "Listing children of $stateID: parPath: ${stateID.file}, mime: $mimeFilter")
        return nodeDB(stateID).treeNodeDao()
            .lsWithMimeFilterFlow(stateID.id, stateID.file, mimeFilter)
    }

    fun listOfflineRoots(
        accountID: StateID,
        sortByCol: String,
        sortByOrder: String
    ): Flow<List<RLiveOfflineRoot>> {
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM RLiveOfflineRoot WHERE " +
                    "status != '${AppNames.OFFLINE_STATUS_LOST}' ORDER BY $sortByCol $sortByOrder"
        )
        return nodeDB(accountID).liveOfflineRootDao().offlineRootQueryF(lsQuery)
    }

    fun listLiveChildren(
        stateID: StateID,
        mime: String,
        orderBy: String,
        orderDir: String,
    ): Flow<List<RTreeNode>> {
        val queryStr = "SELECT * FROM tree_nodes WHERE encoded_state like '${stateID.id}' || '%' " +
                " AND parent_path = '${stateID.file}' AND mime like '$mime' || '%' " +
                " ORDER BY $orderBy $orderDir"
        Log.d(logTag, "Listing live children with query: [$queryStr]")
        return nodeDB(stateID).treeNodeDao().searchQueryFlow(SimpleSQLiteQuery(queryStr))
    }

    fun liveSearch(
        stateID: StateID,
        query: String,
        encodedSortBy: String
    ): Flow<List<RTreeNode>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedSortBy, ListType.DEFAULT)
        val lsQuery = SimpleSQLiteQuery(
            "SELECT * FROM tree_nodes WHERE name like '%${query}%' " +
                    "ORDER BY $sortByCol $sortByOrder LIMIT 100 "
        )
        return nodeDB(stateID).treeNodeDao().searchQueryFlow(lsQuery)
    }

    /* Communicate with the DB using suspend functions */
    suspend fun getNode(stateID: StateID): RTreeNode? = withContext(ioDispatcher) {
        if (stateID == StateID.NONE) {
            null
        } else {
            nodeDB(stateID).treeNodeDao().getNode(stateID.id)
        }
    }

    suspend fun getNodesByUuid(stateID: StateID, uuid: String): List<RTreeNode> =
        withContext(ioDispatcher) {
            nodeDB(stateID).treeNodeDao().getNodesByUuid(uuid)
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
                    address = client.getShareAddress(state.slug, state.file)
                } else {
                    newNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                        address = client.getShareAddress(state.slug, it)
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

    @Throws(SDKException::class)
    suspend fun toggleBookmark(stateID: StateID, newState: Boolean) = withContext(ioDispatcher) {
        try {
            val node1 = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
                ?: return@withContext // TODO throw an error ?

            getNodesByUuid(stateID, node1.uuid).forEach { curr ->
                getClient(stateID).bookmark(stateID.slug, stateID.file, newState)
                curr.setBookmarked(newState)
                curr.localModificationTS = currentTimestamp()
                nodeDB(stateID).treeNodeDao().update(curr)
            }

        } catch (se: SDKException) { // Could not retrieve thumb, failing silently for the end user
            handleSdkException(stateID, "could not toggle bookmark for $stateID", se)
            throw SDKException(se.code, "could not toggle bookmark for $stateID", se)
        }
    }

    suspend fun removeShare(stateID: StateID) = withContext(ioDispatcher) {
        try {
            val client = getClient(stateID)
            val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
            if (client.isLegacy) {
                client.unshare(stateID.slug, stateID.file)
            } else {
                node.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                    client.unshare(stateID.slug, it)
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
        val client = getClient(stateID)

        // We do not want to cancel share creation even if user navigates away.
        return@withContext serviceScope.async {
            // We still put default values. TODO implement user defined details
            try {
                client.share(
                    stateID.slug, stateID.file, stateID.fileName,
                    "Created on ${currentTimestampAsString()}",
                    null, true, true
                )
            } catch (se: SDKException) {
                throw SDKException(se.code, "could create link for $stateID", se)
            } catch (ioe: IOException) {
                throw SDKException(ErrorCodes.internal_error, "could create link for $stateID", ioe)
            }
        }.await()
    }

    suspend fun createFolder(parentID: StateID, folderName: String) =
        withContext(ioDispatcher) {
            try {
                getClient(parentID).mkdir(parentID.slug, parentID.file, folderName)
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
                    targetParent.slug,
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
                    targetParent.slug,
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
            val msg = "Could not refresh folder at $stateID"
            handleSdkException(stateID, msg, e)
            return@withContext Pair(0, msg)
        }
    }

    @Throws(SDKException::class)
    suspend fun tryToCacheNode(stateID: StateID): RTreeNode = withContext(ioDispatcher) {
        val fileNode = getClient(stateID).nodeInfo(stateID.slug, stateID.file)
        val treeNode = RTreeNode.fromFileNode(stateID, fileNode)
        upsertNode(treeNode)
        return@withContext treeNode
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
            return getClient(stateID).nodeInfo(stateID.slug, stateID.file)
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
    @Throws(SDKException::class)
    suspend fun remoteQuery(stateID: StateID, query: String): List<RTreeNode> =
        withContext(ioDispatcher) {
            try {
                val nodes = getClient(stateID).search(stateID.path ?: "/", query, 20)
                    .map {
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
            getClient(stateID).restore(stateID.slug, nodes)

            remoteDelete(stateID)
            treeNodeRepository.persistLocallyModified(node, AppNames.LOCAL_MODIF_DELETE)
        } catch (se: SDKException) {
            se.printStackTrace()
            return@withContext "Could not restore $stateID: ${se.message}"
        }
        return@withContext null
    }

//    @Throws(SDKException::class)
//    suspend fun remoteEmptyRecycle(stateID: StateID) = withContext(ioDispatcher) {
//        getClient(stateID).emptyRecycleBin(stateID.slug)
//    }

    @Throws(SDKException::class)
    suspend fun remoteRename(stateID: StateID, newName: String) = withContext(ioDispatcher) {
        getClient(stateID).rename(stateID.slug, stateID.file, newName)
    }

    @Throws(SDKException::class)
    suspend fun remoteDelete(stateID: StateID) = withContext(ioDispatcher) {
        getClient(stateID).delete(stateID.slug, arrayOf<String>(stateID.file))
    }

    /* Constants and helpers */
    private suspend fun handleSdkException(stateID: StateID, msg: String, se: SDKException) {
        Log.e(logTag, "Error #${se.code}: $msg")
        accountService.notifyError(stateID, msg, se)
    }

    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

    private suspend fun getClient(stateID: StateID): Client {
        return accountService.getClient(stateID)
    }

    fun getLocalFile(item: RTreeNode, type: String): File {
        return File(fileService.getLocalPath(item, type))
    }
}
