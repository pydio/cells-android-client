package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.ROfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.nodes.TreeNodeDao
import com.pydio.android.cells.db.runtime.RJob
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
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class OfflineService(
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

    suspend fun toggleOffline(stateID: StateID, newState: Boolean) = withContext(ioDispatcher) {
        try {
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
        } catch (e: Exception) {
            Log.e(logTag, "could not update offline sync status for ${stateID}: ${e.message}")
            e.printStackTrace()
            return@withContext
        }
    }

    suspend fun updateOfflineRoot(rTreeNode: RTreeNode) {
        updateOfflineRoot(rTreeNode, AppNames.OFFLINE_STATUS_NEW)
    }

    suspend fun updateOfflineRoot(rTreeNode: RTreeNode, status: String) =
        withContext(ioDispatcher) {
            val stateID = rTreeNode.getStateID()
            val db = nodeDB(stateID)
            val offlineDao = db.offlineRootDao()

            val newRoot = ROfflineRoot.fromTreeNode(rTreeNode)
            newRoot.status = status

            // TODO should we check if this node is already a descendant of
            //  an existing offline root ?

            offlineDao.insert(newRoot) // We rely on node UUID and insert REPLACE strategy
            rTreeNode.setOfflineRoot(true)
            treeNodeRepo.persistUpdated(rTreeNode)
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
            treeNodeRepo.persistUpdated(it)
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
    @OptIn(ExperimentalTime::class)
    suspend fun runFullSync(caller: String): RJob? = withContext(ioDispatcher) {

        val label = "Full sync requested by $caller"
        val template = AppNames.JOB_TEMPLATE_FULL_RESYNC

        if (hasExistingJob(label, template, 120)) {
            return@withContext null
        }

        val sessions = accountService.listSessionViews(false).filter {
            // Filter out the accounts with no defined offline root
            nodeDB(it.getStateID()).offlineRootDao().getAllActive().isNotEmpty()
        }

        val job =
            jobService.createAndLaunch(caller, template, label, maxSteps = sessions.size.toLong())
                ?: return@withContext null

        val startTS = timestampForLogMessage()
        val firstMsg = "Full sync started at $startTS by $caller"
        jobService.i(logTag, firstMsg, "${job.jobId}")
        jobService.incrementProgress(job, 0, firstMsg)
        var changeNb = 0
        val timeToSync = measureTimedValue {
            for (session in sessions) {
                val msg = if (session.lifecycleState != AppNames.LIFECYCLE_STATE_PAUSED
                    && session.authStatus == AppNames.AUTH_STATUS_CONNECTED
                ) {
                    changeNb += launchAccountSync(session.getStateID(), caller, job.jobId)
                    "${session.getStateID()} OK"
                } else {
                    "Skip ${session.getStateID()} - session: ${session.lifecycleState}, account: ${session.authStatus} "
                }
                jobService.incrementProgress(job, 1, msg)
            }
        }
        val msg = "Full sync done with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
        val progressMsg = "Full sync terminated at ${timestampForLogMessage()}"
        jobService.done(job, msg, progressMsg)
        jobService.i(logTag, msg, "${job.jobId}")

        return@withContext job
    }

    suspend fun prepareAccountSync(
        stateID: StateID,
        caller: String,
        parentJobId: Long = 0L
    ): Pair<Long, String?> =
        withContext(ioDispatcher) {

            val label = "Account sync for $stateID\nLaunched by $caller"
            val currJobTemplate = getSyncTemplateId(stateID)

            // We first check if a sync is not already running for this account
            if (hasExistingJob(label, currJobTemplate, 120)) {
                return@withContext 0L to "A running job already exists"
            }

            val roots = nodeDB(stateID).offlineRootDao().getAllActive()
            if (roots.isEmpty()) {
                return@withContext 0L to "No offline root is defined for current account"
            }

            val jobId = jobService.create(
                caller,
                currJobTemplate,
                label,
                parentId = parentJobId,
                maxSteps = roots.size.toLong()
            )

            return@withContext jobId to null
        }

    suspend fun performAccountSync(accountID: StateID, jobId: Long, context: Context) =
        withContext(ioDispatcher) {
            val job = jobService.get(jobId) ?: let {
                Log.e(logTag, "No job found for id $jobId, aborting launch...")
                return@withContext
            }
            Log.i(logTag, "####### Sync Worker ########")
            // Log.i(logTag, "### Starting ${job.label} with ID: $jobId")
            jobService.i(logTag, "Starting ${job.label}", "$jobId")

            val roots = nodeDB(accountID).offlineRootDao().getAllActive()
            if (roots.isEmpty()) {
                return@withContext // Should never happen, check has just been done before creating the Job Record
            }


            coroutineService.cellsIoScope.launch {
                jobService.incrementProgress(
                    job,
                    0,
                    context.resources.getString(R.string.sync_tree_walking)
                )

                var changeNb = 0
                for (offlineRoot in roots) {
                    changeNb += syncOfflineRoot(offlineRoot, job)
                }

//            // TODO improve: in fact we only reach this point when the sync has already terminated
//            if (changeNb > 0) {
//                jobService.incrementProgress(job, 0, "Downloading changed files")
//            }
                val msg = "Sync done with $changeNb changes."
                jobService.done(job, msg, "Successfully done")
                jobService.i(logTag, "Terminated ${job.label}", "$jobId")
            }

        }

    @OptIn(ExperimentalTime::class)
    suspend fun launchAccountSync(stateID: StateID, caller: String, parentJobId: Long = 0L): Int =
        withContext(ioDispatcher) {

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

            // Insure we have a connection to this account and correct credentials
            val transport = accountService.getTransport(stateID, true) ?: run {
                // TODO improve error management
                jobService.failed(job.jobId, "No transport for $stateID, cannot launch sync")
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
                        jobService.e(logTag, msg, "${job.jobId}")
                        jobService.failed(job.jobId, msg)
                        return@withContext 0
                    }
                    val msg = "### error while trying to refresh $stateID: ${e.message}"
                    Log.e(logTag, msg)
                    jobService.failed(job.jobId, msg)
                    return@withContext 0
                }
            }

            val timeToSync = measureTimedValue {
                for (offlineRoot in roots) {
                    changeNb += syncOfflineRoot(offlineRoot, job)
                    jobService.incrementProgress(
                        job,
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
            jobService.i(logTag, msg, "${job.jobId}")
            jobService.done(job, msg, "Sync done at ${timestampForLogMessage()}")
            return@withContext changeNb
        }

    @Throws(SDKException::class)
    suspend fun syncOfflineRoot(stateID: StateID) {
        coroutineService.cellsIoScope.launch {
            doSyncOfflineRoot(stateID)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun doSyncOfflineRoot(stateID: StateID): Int {

        val caller = AppNames.JOB_OWNER_USER

        val dao = nodeDB(stateID).offlineRootDao()
        val offlineRoot = dao.get(stateID.id) ?: let {
            Log.w(logTag, "Could not find offline root for $stateID, aborting")
            return 0
        }

        val label = "Sync for $stateID launched by $caller"
        val currJobTemplate = String.format(AppNames.JOB_TEMPLATE_RESYNC, stateID.toString())

        // We first check if a sync is not already running for this account
        if (hasExistingJob(label, currJobTemplate, 120)) {
            return 0
        }

        val job = jobService.createAndLaunch(
            caller,
            currJobTemplate,
            label,
        ) ?: let {
            jobService.e(logTag, "could not create sync job for $stateID ")
            return 0
        }

        val changeNb: Int
        val timeToSync = measureTimedValue {
            changeNb = syncOfflineRoot(offlineRoot, job)
        }
        val msg = "Synced $stateID with $changeNb changes in ${timeToSync.duration.inWholeSeconds}s"
        jobService.i(logTag, msg, "${job.jobId}")
        jobService.done(job, msg, "Sync done at ${timestampForLogMessage()}")

        return changeNb
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncOfflineRoot(offlineRoot: ROfflineRoot, job: RJob): Int =
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

                val fileDL = FileDownloader(job)
                var changeNb = 0
                val timeToSync = measureTimedValue {
                    changeNb += syncNodeAt(treeNode, client, treeNodeDao, fileDL)
                }
                val msg = "walked  $stateID in ${timeToSync.duration.inWholeSeconds}s"
                jobService.d(logTag, msg, job.jobId.toString())
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
                    runningJob.doneTimestamp = currentTimestamp()
                    runningJob.message = msg
                    runningJob.status = AppNames.JOB_STATUS_TIMEOUT
                    jobService.update(runningJob)
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
