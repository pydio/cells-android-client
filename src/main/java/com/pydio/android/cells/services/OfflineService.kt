package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.ROfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.nodes.TreeNodeDao
import com.pydio.android.cells.transfer.FileDownloader
import com.pydio.android.cells.transfer.TreeDiff
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.timestampForLogMessage
import com.pydio.cells.api.Client
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

class OfflineService(
    private val context: Context,
    private val coroutineService: CoroutineService,
    private val credentialService: AppCredentialService,
    private val accountService: AccountService,
    private val treeNodeRepo: TreeNodeRepository,
    private val jobService: JobService,
) {
    private val logTag = "OfflineService"

    private val ioDispatcher = coroutineService.ioDispatcher

    fun getSyncTemplateId(stateID: StateID): String {
        return "${AppNames.JOB_TEMPLATE_RESYNC}-$stateID"
    }

    // CRUD For the Offline Roots

    @Throws(SDKException::class)
    suspend fun toggleOffline(stateID: StateID, newState: Boolean) = withContext(ioDispatcher) {
        val node = nodeDB(stateID).treeNodeDao().getNode(stateID.id) ?: return@withContext
        if (node.isOfflineRoot()) {
            if (!newState) {
                removeOfflineRoot(stateID)
            }
        } else {
            if (newState) {
                updateOfflineRoot(node)
            }
        }
    }

    suspend fun updateOfflineRoot(
        rTreeNode: RTreeNode,
        status: String = AppNames.OFFLINE_STATUS_NEW
    ) =
        withContext(ioDispatcher) {
            val stateID = rTreeNode.getStateID()
            val db = nodeDB(stateID)
            val offlineDao = db.offlineRootDao()

            val newRoot = ROfflineRoot.fromTreeNode(rTreeNode)
            newRoot.status = status

            // TODO should we check if this node is already a descendant of an existing offline root ?
            offlineDao.insert(newRoot) // We rely on node UUID and insert REPLACE strategy
            rTreeNode.setOfflineRoot(true)
            treeNodeRepo.persistUpdated(rTreeNode, currentTimestamp())
        }

    suspend fun removeOfflineRoot(stateID: StateID) = withContext(ioDispatcher) {
        val db = nodeDB(stateID)
        val offlineDao = db.offlineRootDao()

        if (offlineDao.get(stateID.id) == null) { // Nothing to do
            return@withContext
        }

        offlineDao.delete(stateID.id)
        db.treeNodeDao().getNode(stateID.id)?.let {
            it.setOfflineRoot(false)
            // TODO insure it has no side effects when we are offline and the node also get updated on the server side (by SO else)
            treeNodeRepo.persistUpdated(it, currentTimestamp())
        }

        // TODO also clean file system ?
    }

//    // Finalize this: we should try to move already existing cache file and index
////   rather than deleting / recreating the offline root
//    suspend fun moveOfflineRoot(rTreeNode: RTreeNode, offlineRoot: ROfflineRoot) =
//        withContext(ioDispatcher) {
//            val stateID = rTreeNode.getStateID()
//            val db = nodeDB(stateID)
//            val offlineDao = db.offlineRootDao()
//            offlineRoot.encodedState = rTreeNode.encodedState
//            offlineDao.insert(offlineRoot) // We rely on node UUID and insert REPLACE strategy
//            rTreeNode.setOfflineRoot(true)
//            treeNodeRepo.persistUpdated(rTreeNode)
//        }

    /* Offline synchronisation */
    suspend fun runFullSync(caller: String): Long = withContext(ioDispatcher) {

        val label = "Full sync requested by $caller"
        val template = AppNames.JOB_TEMPLATE_FULL_RESYNC

        if (hasExistingJob(label, template, 120)) {
            throw SDKException(
                ErrorCodes.illegal_argument,
                "Cannot launch, sync is already running"
            )
        }

        val sessions = accountService.listSessionViews(false).filter {
            // Filter out the accounts with no defined offline root
            nodeDB(it.getStateID()).offlineRootDao().getAllActive().isNotEmpty()
        }

        val jobID =
            jobService.createAndLaunch(caller, template, label, maxSteps = sessions.size.toLong())

        val startTS = timestampForLogMessage()
        val firstMsg = "Full sync started at $startTS by $caller"
        jobService.i(logTag, firstMsg, "Job #$jobID")
        jobService.incrementProgress(jobID, 0, firstMsg)
        var changeNb = 0
        val timeToSync = measureTimedValue {
            for (session in sessions) {
                val msg =
                    if (session.isLoggedIn() && session.lifecycleState != AppNames.LIFECYCLE_STATE_PAUSED) {
                        changeNb += launchAccountSync(session.getStateID(), caller, jobID)
                        "${session.getStateID()} OK"
                    } else {
                        "Skip ${session.getStateID()} - session: ${session.lifecycleState}, account: ${session.authStatus} "
                    }
                jobService.incrementProgress(jobID, 1, msg)
            }
        }
        val msg = "Full sync done with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
        val progressMsg = "Full sync terminated at ${timestampForLogMessage()}"
        jobService.done(jobID, msg, progressMsg)
        jobService.i(logTag, msg, "Job #$jobID")

        return@withContext jobID
    }

    @Throws(SDKException::class)
    suspend fun prepareAccountSync(
        stateID: StateID,
        caller: String,
        parentJobId: Long = -1L
    ): Long = withContext(ioDispatcher) {

        val label = "Account sync for $stateID\nLaunched by $caller"
        val currJobTemplate = getSyncTemplateId(stateID)

        // We first check if a sync is not already running for this account
        if (hasExistingJob(label, currJobTemplate, 120)) {
            throw SDKException(ErrorCodes.init_failed, "A running job already exists")
        }

        val roots = nodeDB(stateID).offlineRootDao().getAllActive()
        if (roots.isEmpty()) {
            throw SDKException(
                ErrorCodes.init_failed,
                "No offline root is defined for current account"
            )
        }

        Log.e(logTag, "Creating job with ${roots.size} steps - Parent: $parentJobId")

        return@withContext jobService.create(
            caller,
            currJobTemplate,
            label,
            parentId = parentJobId,
            maxSteps = roots.size.toLong()
        )
    }

    suspend fun performAccountSync(accountID: StateID, jobID: Long) =
        withContext(ioDispatcher) {
            val job = jobService.get(jobID) ?: let {
                val msg = "No job found for id $jobID, aborting launch..."
                Log.e(logTag, msg)
                throw SDKException(ErrorCodes.init_failed, msg)
            }
            val roots = nodeDB(accountID).offlineRootDao().getAllActive()
            if (roots.isEmpty()) {
                return@withContext // Should never happen, check has just been done before creating the Job Record
            }
            jobService.i(logTag, "Starting ${job.label}", "$jobID")

            val realJob = coroutineService.cellsIoScope.launch {

                val prefix = context.resources.getString(R.string.sync_tree_walking)
                // Put total to -1 to have an undefined progress
                jobService.updateTotal(jobID, -1, JobStatus.PROCESSING.id, prefix)

                var changeNb = 0
                for (offlineRoot in roots) {
                    jobService.incrementProgress(
                        jobID,
                        0,
                        "$prefix - ${offlineRoot.getStateID().fileName}"
                    )
                    changeNb += syncOfflineRoot(offlineRoot, jobID)
                }

                val msg = "Sync done with $changeNb changes."
                jobService.done(jobID, msg, "Successfully done")
                jobService.i(logTag, "Terminated ${job.label}", "$jobID")
            }
            Log.i(logTag, "## Sync Worker has been launched: $realJob")

        }

    private suspend fun launchAccountSync(
        stateID: StateID,
        caller: String,
        parentJobID: Long = 0L
    ): Int = withContext(ioDispatcher) {

        val label = "Account sync for $stateID launched by $caller"
        val currJobTemplate = getSyncTemplateIdForAccount(stateID)

        // We first check if a sync is not already running for this account
        if (hasExistingJob(label, currJobTemplate, 120)) {
            return@withContext 0
        }

        var changeNb = 0
        val roots = nodeDB(stateID).offlineRootDao().getAllActive()
        if (roots.isEmpty()) {
            return@withContext 0
        }
        jobService.d(logTag, "Syncing: $stateID", "$parentJobID")

        val jobID = jobService.createAndLaunch(
            caller,
            currJobTemplate,
            label,
            parentId = parentJobID,
            maxSteps = roots.size.toLong()
        )

        // Insure we have a connection to this account and correct credentials
        val transport = accountService.getTransport(stateID, true) ?: run {
            // TODO improve error management
            jobService.failed(jobID, "No transport for $stateID, cannot launch sync")
            return@withContext 1
        }
        if (transport is CellsTransport) {
            // also insure we have credentials
            try {
                credentialService.requestRefreshToken(stateID)
                delay(2000)
            } catch (e: SDKException) {
                if (e.code == ErrorCodes.no_token_available) {
                    Log.e(logTag, "### No Token Available error, about to logout $stateID")
                    accountService.logoutAccount(stateID.account())
                    val msg = "No Token for $stateID aborting: login out account and abort "
                    jobService.e(logTag, msg, "Job #${jobID}")
                    jobService.failed(jobID, msg)
                    return@withContext 0
                }
                val msg = "### error while trying to refresh $stateID: ${e.message}"
                Log.e(logTag, msg)
                jobService.failed(jobID, msg)
                return@withContext 0
            }
        }

        val timeToSync = measureTimedValue {
            for (offlineRoot in roots) {
                changeNb += syncOfflineRoot(offlineRoot, jobID)
                jobService.incrementProgress(
                    jobID,
                    1,
                    "Sync done for ${offlineRoot.getStateID()}"
                )
                // TODO improve this
                if (transport is CellsTransport) {
                    // also insure we have credentials
                    credentialService.requestRefreshToken(stateID)
                }
            }
        }
        val msg =
            "Sync done with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s for $stateID"
        jobService.i(logTag, msg, "Job #$jobID")
        jobService.done(jobID, msg, "Sync done at ${timestampForLogMessage()}")
        return@withContext changeNb
    }

    @Throws(SDKException::class)
    suspend fun launchOfflineRootSync(stateID: StateID): Long = withContext(ioDispatcher) {
        val caller = AppNames.JOB_OWNER_USER
        val dao = nodeDB(stateID).offlineRootDao()
        val offlineRoot = dao.get(stateID.id) ?: let {
            throw SDKException(
                ErrorCodes.illegal_argument,
                "Could not find offline root for $stateID, aborting"
            )
        }

        val label = "Sync for $stateID launched by $caller"
        val currJobTemplate = String.format(AppNames.JOB_TEMPLATE_RESYNC, stateID.toString())

        // We first check if a sync is not already running for this account
        if (hasExistingJob(label, currJobTemplate, 120)) {
            throw SDKException(ErrorCodes.illegal_argument, "Sync for $stateID is already running")
        }

        val jobID = jobService.createAndLaunch(
            caller,
            currJobTemplate,
            label,
        )
        jobService.incrementProgress(jobID, 0, label)

        // Launch the real processing in a background thread
        coroutineService.cellsIoScope.launch {
            val changeNb: Int
            val timeToSync = measureTimedValue {
                changeNb = syncOfflineRoot(offlineRoot, jobID)
            }
            val msg =
                "Synced ${stateID.fileName} with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
            jobService.i(logTag, msg, "Job #$jobID")
            jobService.done(jobID, msg, "Sync done at ${timestampForLogMessage()}")
        }
        return@withContext jobID
    }

    private suspend fun syncOfflineRoot(offlineRoot: ROfflineRoot, jobID: Long): Int =
        withContext(ioDispatcher) {
            val stateID = offlineRoot.getStateID()
            try {
                val client = getClient(stateID)
                val db = nodeDB(stateID)
                val treeNodeDao = db.treeNodeDao()
                val offlineDao = db.offlineRootDao()

                val treeNode = treeNodeDao.getNode(offlineRoot.encodedState)
                    ?: run {
                        val nodeInfo = client.nodeInfo(stateID.slug, stateID.file)
                            ?: run {
                                // Remote node has also disappeared on server
                                offlineRoot.status = AppNames.OFFLINE_STATUS_LOST
                                offlineRoot.lastCheckTS = currentTimestamp()
                                return@withContext 0
                            }
                        RTreeNode.fromFileNode(stateID, nodeInfo)
                    }

                val fileDL = FileDownloader(stateID, jobID)
                var changeNb = 0
                val timeToSync = measureTimedValue {
                    changeNb += syncNodeAt(treeNode, client, treeNodeDao, fileDL)
                }
                val msg = "walked  $stateID in ${timeToSync.duration.inWholeSeconds}s"
                jobService.d(logTag, msg, "Job #$jobID")
                fileDL.walkingDone()
                fileDL.manualJoin()

                if (!fileDL.isFailed()) {
                    if (changeNb > 0) {
                        offlineRoot.localModificationTS = currentTimestamp()
                        offlineRoot.message = null // TODO double check
                    }
                    offlineRoot.lastCheckTS = currentTimestamp()
                    offlineRoot.status = AppNames.OFFLINE_STATUS_ACTIVE
                    offlineDao.update(offlineRoot)

                    // TODO add more info on the corresponding root RTreeNode ??
                    treeNode.setOfflineRoot(true)
                    treeNodeRepo.persistUpdated(treeNode)
                }
                return@withContext changeNb
            } catch (se: SDKException) {
                Log.e(logTag, "could update offline sync status for $stateID")
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

    private fun getSyncTemplateIdForAccount(accountID: StateID): String {
        return String.format(AppNames.JOB_TEMPLATE_RESYNC, accountID.toString())
    }

    private suspend fun hasExistingJob(
        label: String,
        template: String,
        timeoutSinceUpdated: Int = 120,
        timeoutSinceStarted: Int = 300,
    ): Boolean {

        // We first check if a sync is not already running for this account
        var runningJobs = jobService.getRunningJobs(template)
        if (runningJobs.isNotEmpty()) {
            // We check for old jobs
            for (runningJob in runningJobs) {

                val (isTimedOut, msg) = if (runningJob.updateTimestamp > 0) {
                    val dur = currentTimestamp() - runningJob.updateTimestamp
                    (dur > timeoutSinceUpdated) to "Timeout: no update since $dur seconds"
                } else {
                    val dur = currentTimestamp() - runningJob.startTimestamp
                    (dur > timeoutSinceStarted) to "Timeout: job started with no update since $dur seconds"
                }
                if (isTimedOut) {
                    jobService.updateById(runningJob.jobId) { currJob ->
                        currJob.doneTimestamp = currentTimestamp()
                        currJob.message = msg
                        currJob.status = JobStatus.TIMEOUT.id
                        currJob
                    }
                    jobService.w(logTag, msg, "${runningJob.jobId}")
                }
            }

            // We re-query the DB to see if we still have old jobs and cancel the launch in such case
            runningJobs = jobService.getRunningJobs(template)
            if (runningJobs.isNotEmpty()) {
                var msg =
                    "Cannot start job [$label], job ${runningJobs[0].jobId} is already running"
                if (runningJobs.size > 1) {
                    msg += " (plus ${runningJobs.size - 1} others)"
                }
                jobService.w(logTag, msg, "[not started]")
                return true
            }
        }

        return false
    }

    /* Constants and helpers */
    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepo.nodeDB(stateID)
    }

    private suspend fun getClient(stateID: StateID): Client {
        return accountService.getClient(stateID)
    }
}
