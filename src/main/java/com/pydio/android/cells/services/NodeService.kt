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
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.transfer.FileDownloader
import com.pydio.android.cells.transfer.TreeDiff
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.currentTimestampAsString
import com.pydio.android.cells.utils.logException
import com.pydio.android.cells.utils.parseOrder
import com.pydio.android.cells.utils.timestampForLogMessage
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class NodeService(
    private val prefs: CellsPreferences,
    private val treeNodeRepository: TreeNodeRepository,
    private val jobService: JobService,
    private val accountService: AccountService,
    private val fileService: FileService,
) {

    private val logTag = NodeService::class.simpleName
    private val nodeServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + nodeServiceJob)

    /* Expose DB content as LiveData for the ViewModels */

    fun ls(stateID: StateID): LiveData<List<RTreeNode>> {
        var encoded = prefs.getPreference(AppNames.PREF_KEY_CURR_RECYCLER_ORDER)
        if (Str.empty(encoded)) {
            encoded = "${AppNames.DEFAULT_SORT_BY}||${AppNames.DEFAULT_SORT_BY_DIR}"
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
    suspend fun upsertNode(newNode: RTreeNode, isDiffRoot: Boolean = false) =
        withContext(Dispatchers.IO) {

            val state = newNode.getStateID()
            val currSession = treeNodeRepository.sessions[newNode.getStateID().accountId]
                ?: throw java.lang.IllegalStateException("No session found in cache for ${newNode.getStateID().accountId}")
            val ndb = nodeDB(state)

            // Also cache offline status and public link URL locally
            if (!currSession.isRemoteLegacy) {
                ndb.offlineRootDao().getByUuid(newNode.uuid)?.let {
                    newNode.setOfflineRoot(true)
                }
            }
            var addr: String? = null
            val isShared =
                newNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARED, "false") == "true"
            if (isShared) {
                val client = accountService.getClient(state)
                if (client.isLegacy) {
                    addr = client.getShareAddress(state.workspace, state.file)
                } else {
                    newNode.properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_UUID)?.let {
                        addr = client.getShareAddress(state.workspace, it)
                    }
                }
            }
            newNode.setShared(isShared, addr)

            newNode.localModificationTS = newNode.remoteModificationTS
            newNode.localModificationStatus = null

            if (newNode.isFile() || isDiffRoot) {
                // We only update last check TS on folder if explicitly required by param,
                // after checking the full content of a folder, in order to differentiate
                // not-yet loaded folders from empty ones.
                newNode.lastCheckTS = currentTimestamp()
            }

            val old = ndb.treeNodeDao().getNode(newNode.encodedState)
            if (old == null) {
                ndb.treeNodeDao().insert(newNode)
            } else {
                // TODO double check that we do not loose any info
                //    (not true with RLocalFile object anymore) -> Typically we force re-download of thumbs at each update
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

    suspend fun toggleShared(rTreeNode: RTreeNode): String? = withContext(Dispatchers.IO) {
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

    suspend fun toggleOffline(rTreeNode: RTreeNode) = withContext(Dispatchers.IO) {
        val stateID = rTreeNode.getStateID()
        try {
            if (rTreeNode.isOfflineRoot()) {
                removeOfflineRoot(stateID)
            } else {
                updateOfflineRoot(rTreeNode)
            }
        } catch (e: Exception) {
            Log.e(logTag, "could update offline sync status for ${stateID}: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }

    private suspend fun removeOfflineRoot(stateID: StateID) = withContext(Dispatchers.IO) {
        val db = nodeDB(stateID)
        val offlineDao = db.offlineRootDao()

        if (offlineDao.get(stateID.id) == null) { // Nothing to do
            return@withContext
        }

        offlineDao.delete(stateID.id)
        db.treeNodeDao().getNode(stateID.id)?.let {
            it.setOfflineRoot(false)
            persistUpdated(it)
        }

        // TODO also clean file system ?
    }

    suspend fun updateOfflineRoot(rTreeNode: RTreeNode, status: String) =
        withContext(Dispatchers.IO) {
            val stateID = rTreeNode.getStateID()
            val db = nodeDB(stateID)
            val offlineDao = db.offlineRootDao()

            val newRoot = ROfflineRoot.fromTreeNode(rTreeNode)
            newRoot.status = status

            // TODO should we check if this node is already a descendant of
            //  an existing offline root ?

            offlineDao.insert(newRoot) // We rely on node UUID and insert REPLACE strategy
            rTreeNode.setOfflineRoot(true)
            persistUpdated(rTreeNode)
        }

    suspend fun updateOfflineRoot(rTreeNode: RTreeNode) {
        updateOfflineRoot(rTreeNode, AppNames.OFFLINE_STATUS_NEW)
    }

    /* Offline synchronisation */
    @OptIn(ExperimentalTime::class)
    suspend fun runFullSync(caller: String): RJob? {

        // We first check if a full sync is already running
        val runningJobs = jobService.getRunningJobs(AppNames.JOB_TEMPLATE_FULL_RESYNC)
        if (runningJobs.isNotEmpty()) {
            val startTS = timestampForLogMessage()
            jobService.w(
                logTag,
                "Cannot start full sync requested by $caller at $startTS, " +
                        "${runningJobs.size} job(s) are already running",
                null
            )
            return null
        }

        val sessions = accountService.listSessionViews(false)
        // TODO add a filter to get rid of the sessions that have no defined offline roots

        val job = jobService.createAndLaunch(
            caller,
            AppNames.JOB_TEMPLATE_FULL_RESYNC,
            "Full sync requested by $caller",
            sessions.size.toLong(),
        ) ?: return null


        val startTS = timestampForLogMessage()
        jobService.i(logTag, "Full sync started at $startTS by $caller", "${job.jobId}")

        var changeNb = 0
        val timeToSync = measureTimedValue {
            for (session in sessions) {
                if (session.lifecycleState != AppNames.LIFECYCLE_STATE_PAUSED
                    && session.authStatus == AppNames.AUTH_STATUS_CONNECTED
                ) {
                    changeNb += runAccountSync(session.getStateID(), caller, job.jobId)
                    jobService.incrementProgress(job, 1, "${session.getStateID()} OK")
                }
            }
        }
        val msg = "Full sync done with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
        val progressMsg = "Full sync terminated at ${timestampForLogMessage()}"
        jobService.done(job, msg, progressMsg)
        jobService.i(logTag, msg, "${job.jobId}")

        return job
    }

    @OptIn(ExperimentalTime::class)
    suspend fun runAccountSync(stateID: StateID, caller: String, parentJobId: Long = 0L): Int =
        withContext(Dispatchers.IO) {

            val label = "Account sync for $stateID launched by $caller"
            val currJobTemplate = String.format(AppNames.JOB_TEMPLATE_RESYNC, stateID.toString())

            // We first check if a sync is not already running for this account
            if (hasExistingJob(label, currJobTemplate, 120)) {
                return@withContext 0
            }

            var changeNb = 0
            val roots = nodeDB(stateID).offlineRootDao().getAll()
            if (roots.isEmpty()) {
                return@withContext 0
            }
            jobService.d(logTag, "Syncing: $stateID", "$parentJobId")

            val job = jobService.createAndLaunch(
                caller,
                currJobTemplate,
                label,
                parentId = parentJobId,
                maxSteps = roots.size.toLong()
            ) ?: let {
                jobService.e(logTag, "could not create account sync job for $stateID ")
                return@withContext 0
            }
            val timeToSync = measureTimedValue {
                for (offlineRoot in roots) {
                    changeNb += syncOfflineRoot(offlineRoot, job)
                    jobService.incrementProgress(
                        job,
                        1,
                        "Sync done for ${offlineRoot.getStateID()}"
                    )
                }
            }
            val msg =
                "Sync done with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s for ${stateID}"
            jobService.i(logTag, msg, "${parentJobId}")
            jobService.done(job, msg, "Sync done at ${timestampForLogMessage()}")
            return@withContext changeNb
        }

    @OptIn(ExperimentalTime::class)
    suspend fun syncOfflineRoot(rTreeNode: RTreeNode): Int = withContext(Dispatchers.IO) {

        val stateID = rTreeNode.getStateID()
        val caller = AppNames.JOB_OWNER_USER

        val dao = nodeDB(stateID).offlineRootDao()
        val offlineRoot = dao.get(rTreeNode.encodedState) ?: let {
            Log.w(logTag, "Could not find offline root for $stateID, aborting")
            return@withContext 0
        }

        val label = "Sync for $stateID launched by $caller"
        val currJobTemplate = String.format(AppNames.JOB_TEMPLATE_RESYNC, stateID.toString())

        // We first check if a sync is not already running for this account
        if (hasExistingJob(label, currJobTemplate, 120)) {
            return@withContext 0
        }

        var changeNb = 0
        val job = jobService.createAndLaunch(
            caller,
            currJobTemplate,
            label,
        ) ?: let {
            jobService.e(logTag, "could not create sync job for $stateID ")
            return@withContext 0
        }

        val timeToSync = measureTimedValue {
            changeNb = syncOfflineRoot(offlineRoot, job)
        }

        val msg =
            "Sync prepared for $stateID with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
        jobService.i(logTag, msg, "${job.jobId}")
        jobService.done(job, msg, "Sync done at ${timestampForLogMessage()}")

        return@withContext changeNb

    }


    private suspend fun hasExistingJob(
        label: String,
        template: String,
        timeoutInSeconds: Int
    ): Boolean {

        // We first check if a sync is not already running for this account
        var runningJobs = jobService.getRunningJobs(template)
        if (runningJobs.isNotEmpty()) {
            // We check for old jobs
            for (runningJob in runningJobs) {
                val timeSinceStart = currentTimestamp() - runningJob.startTimestamp
                if (timeSinceStart > timeoutInSeconds) {
                    runningJob.doneTimestamp = currentTimestamp()
                    runningJob.message = "Timeout after $timeSinceStart seconds"
                    runningJob.status = AppNames.JOB_STATUS_TIMEOUT
                    jobService.update(runningJob)
                }
            }

            // We re-query the DB to see if we still have old jobs and cancel the launch in such case
            runningJobs = jobService.getRunningJobs(template)
            if (runningJobs.isNotEmpty()) {
                val startTS = timestampForLogMessage()
                jobService.w(
                    logTag,
                    "Cannot start job [$label], ${runningJobs.size} job(s) are already running",
                    null
                )
                return true
            }
        }

        return false
    }

    private suspend fun syncOfflineRoot(offlineRoot: ROfflineRoot, job: RJob): Int =
        withContext(Dispatchers.IO) {
            val stateID = offlineRoot.getStateID()
            try {
                val client = getClient(stateID)
                val db = nodeDB(stateID)
                val treeNodeDao = db.treeNodeDao()
                val offlineDao = db.offlineRootDao()

                val treeNode = treeNodeDao.getNode(offlineRoot.encodedState)
                    ?: run {
                        val nodeInfo = client.nodeInfo(stateID.workspace, stateID.file)
                            ?: run {
                                // Remote node has also disappeared on server
                                offlineRoot.status = AppNames.OFFLINE_STATUS_LOST
                                offlineRoot.lastCheckTS = currentTimestamp()
                                return@withContext 0
                            }
                        RTreeNode.fromFileNode(stateID, nodeInfo)
                    }

                val fileDL = FileDownloader(job)
                val changeNb = syncNodeAt(treeNode, client, treeNodeDao, fileDL)
                fileDL.walkingDone()
                fileDL.manualJoin()

                if (changeNb > 0) {
                    offlineRoot.localModificationTS = currentTimestamp()
                    offlineRoot.message = null // TODO double check
                }
                offlineRoot.lastCheckTS = currentTimestamp()
                offlineRoot.status = AppNames.OFFLINE_STATUS_ACTIVE
                offlineDao.update(offlineRoot)

                // TODO add more info on the corresponding root RTreeNode ??
                treeNode.setOfflineRoot(true)
                persistUpdated(treeNode)
                return@withContext changeNb
            } catch (se: SDKException) {
                Log.e(logTag, "could update offline sync status for " + stateID.id)
                se.printStackTrace()
                return@withContext 0
            }
        }

    private suspend fun syncNodeAt(
        rTreeNode: RTreeNode,
        client: Client,
        dao: TreeNodeDao,
        fileDL: FileDownloader
    ): Int {

        val stateID = rTreeNode.getStateID()

        // First re-sync current level
        val treeDiff = TreeDiff(stateID, client, dao, fileDL)
        var changeNb = treeDiff.compareWithRemote()

        if (rTreeNode.isFolder()) {
            // Then retrieve child folders and call re-sync on each one
            val children = nodeDB(stateID).treeNodeDao()
                .listWithMime(stateID.id, stateID.file, SdkNames.NODE_MIME_FOLDER)
            for (child in children) {
                changeNb += syncNodeAt(child, client, dao, fileDL)
            }
        }
        return changeNb
    }

    fun enqueueDownload(stateID: StateID, uri: Uri) {
        serviceScope.launch {
            saveToSharedStorage(stateID, uri)
        }
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

    // Handle communication with the remote server to refresh locally stored data.

    /**
     * Retrieve the meta of all readable nodes that are at the passed stateID.
     * Files and thumbs are lazily retrieved by Glide (for images) or upon user request (for all
     * other files).
     */
    suspend fun pull(stateID: StateID): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val client = getClient(stateID)
            val dao = nodeDB(stateID).treeNodeDao()

            // WARNING: this browse **all** files that are in the folder
            val folderDiff = TreeDiff(stateID, client, dao, null)
            val changeNb = folderDiff.compareWithRemote()
            return@withContext Pair(changeNb, null)
        } catch (e: SDKException) {
            val msg = "could not perform ls for ${stateID.id}"
            handleSdkException(stateID, msg, e)
            return@withContext Pair(0, msg)
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

    private suspend fun getNodeInfo(stateID: StateID): FileNode? {
        try {
            return getClient(stateID).nodeInfo(stateID.workspace, stateID.file)
        } catch (e: SDKException) {
            handleSdkException(stateID, "could not getNodeInfo for $stateID", e)
            throw e
        }
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
            val dirName = treeNodeRepository.sessions[account.id]?.dirName
                ?: throw IllegalStateException("Cannot clean cache: no record found for $stateID")
            fileService.cleanFileCacheFor(account, dirName)
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

        // FIXME
        return true

//        // Compare with remote if possible
//        val remoteStats = statRemoteNode(StateID.fromId(rTreeNode.encodedState)) ?: return null
//        if (rTreeNode.localFileType != AppNames.LOCAL_FILE_TYPE_NONE
//            && rTreeNode.localModificationTS >= remoteStats.getmTime()
//        ) {
//            fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_CACHE).let {
//                val file = File(it)
//                // TODO at this point we are not 100% sure the local file
//                //  is in-line with remote, typically if update is in process
//                if (file.exists()) {
//                    return true
//                }
//            }
//        }
//        return false
    }

    /**
     * Returns the local file to be opened if it exists, optionally after checking
     * if it is still up to date based on:
     * - modif time
     * - e-tag
     * - size
     */
    suspend fun getLocalFile(rTreeNode: RTreeNode, checkUpToDate: Boolean): File? =
        withContext(Dispatchers.IO) {
            val file = File(fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_FILE))
            if (!file.exists()) {
                return@withContext null
            }

            if (!checkUpToDate) {
                return@withContext file
            }
            try {
                // Compare with remote if possible
                val remote = getNodeInfo(rTreeNode.getStateID())
                // We cannot stat remote, but we have a file let's open this one FIXME
                    ?: return@withContext file

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

//    suspend fun getOrDownloadFileToCache(rTreeNode: RTreeNode): File? =
//
//        withContext(Dispatchers.IO) {
//            Log.i(logTag, "In getOrDownloadFileToCache for ${rTreeNode.name}")
//            // TODO improve check to decide whether we should download the full file or not
//            val isOK = isCacheVersionUpToDate(rTreeNode)
//            when {
//                isOK == null && rTreeNode.localFileType != AppNames.LOCAL_FILE_TYPE_NONE
//                -> fileService.getLocalPath(rTreeNode, AppNames.LOCAL_FILE_TYPE_CACHE)
//                    .let { return@withContext File(it) }
//                isOK == null && rTreeNode.localFileType == AppNames.LOCAL_FILE_TYPE_NONE
//                -> {
//                }
//                isOK ?: false
//                -> return@withContext File(
//                    fileService.getLocalPath(
//                        rTreeNode,
//                        AppNames.LOCAL_FILE_TYPE_CACHE
//                    )
//                )
//            }
//
//            Log.i(logTag, "... Launching download for ${rTreeNode.name}")
//
//            val stateID = rTreeNode.getStateID()
//            val baseDir =
//                fileService.dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_CACHE)
//            val targetFile = File(baseDir, stateID.path.substring(1))
//            targetFile.parentFile!!.mkdirs()
//            var out: FileOutputStream? = null
//
//            try {
//                out = FileOutputStream(targetFile)
//
//                // TODO handle progress
//                getClient(stateID).download(stateID.workspace, stateID.file, out, null)
//
//                // Success persist change
//                rTreeNode.localFileType = AppNames.LOCAL_FILE_TYPE_CACHE
//                rTreeNode.localModificationTS = rTreeNode.remoteModificationTS
//                nodeDB(stateID).treeNodeDao().update(rTreeNode)
//                Log.i(logTag, "... download done for ${rTreeNode.name}")
//            } catch (se: SDKException) {
//                // Could not retrieve thumb, failing silently for the end user
//                val msg = "could not perform DL for " + stateID.id
//                handleSdkException(stateID, msg, se)
//                return@withContext null
//            } catch (ioe: IOException) {
//                // TODO handle this: what should we do ?
//                Log.e(logTag, "cannot write at ${targetFile.absolutePath}: ${ioe.message}")
//                ioe.printStackTrace()
//                return@withContext null
//            } finally {
//                IoHelpers.closeQuietly(out)
//            }
//            targetFile
//        }

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
                                AppNames.LOCAL_FILE_TYPE_FILE
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
                val nodes = getClient(stateID).search(stateID.path, query, 20)
                    .map { RTreeNode.fromFileNode(stateID, it) }

                // We already insert found nodes in the cache to ease following user action
                for (node in nodes) {
                    getNode(node.getStateID()) ?: upsertNode(node)
                }

                return@withContext nodes
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
    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

    private fun getClient(stateId: StateID): Client {
        return accountService.getClient(stateId)
    }

    private fun persistUpdated(rTreeNode: RTreeNode) {
        rTreeNode.localModificationTS = rTreeNode.remoteModificationTS
        val dao = nodeDB(rTreeNode.getStateID()).treeNodeDao()
        dao.getNode(rTreeNode.getStateID().id)
            ?.let { dao.update(rTreeNode) }
            ?: let { dao.insert(rTreeNode) }
    }

    private fun persistLocallyModified(rTreeNode: RTreeNode, modificationType: String) {
        rTreeNode.localModificationTS = currentTimestamp()
        rTreeNode.localModificationStatus = modificationType
        nodeDB(rTreeNode.getStateID()).treeNodeDao().update(rTreeNode)
    }

    private suspend fun handleSdkException(stateID: StateID, msg: String, se: SDKException) {
        Log.e(logTag, "Error #${se.code}: $msg")
        se.printStackTrace()
        accountService.notifyError(stateID, se.code)
    }

    private fun getLocalFile(item: RTreeNode, type: String): File {
// Trick so that we do not store offline files also in the cache... double check
//        if (type == AppNames.LOCAL_FILE_TYPE_OFFLINE ||
//            type == AppNames.LOCAL_FILE_TYPE_CACHE // FIXME &&
////             item.localFileType == AppNames.LOCAL_FILE_TYPE_OFFLINE
//        ) {
//            return File(fileService.getLocalPath(item, AppNames.LOCAL_FILE_TYPE_OFFLINE))
//        }
        return File(fileService.getLocalPath(item, type))
    }
}
