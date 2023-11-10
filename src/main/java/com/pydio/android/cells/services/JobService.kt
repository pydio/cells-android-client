package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manage long running background jobs with progress */
class JobService(
    coroutineService: CoroutineService,
    runtimeDB: RuntimeDB
) {

    private val serviceScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    private val jobDao = runtimeDB.jobDao()
    private val logDao = runtimeDB.logDao()

    suspend fun get(jobId: Long): RJob? = withContext(ioDispatcher) { jobDao.getById(jobId) }

    suspend fun create(
        owner: String,
        template: String,
        label: String,
        parentId: Long = -1,
        maxSteps: Long = -1
    ): Long = withContext(ioDispatcher) {
        val newJob = RJob.create(owner, template, label, parentId)
        newJob.total = maxSteps
        newJob.updateTimestamp = currentTimestamp()
        return@withContext jobDao.insert(newJob)
    }

    suspend fun createAndLaunch(
        owner: String,
        template: String,
        label: String,
        parentId: Long = -1,
        maxSteps: Long = -1
    ): Long = withContext(ioDispatcher) {
        val newJob = RJob.create(owner, template, label, parentId)
        newJob.total = maxSteps
        newJob.status = JobStatus.PROCESSING.id
        newJob.startTimestamp = currentTimestamp()
        newJob.updateTimestamp = currentTimestamp()
        return@withContext jobDao.insert(newJob)
    }

    private val lock = Any()
    suspend fun updateById(jobID: Long, handler: (RJob) -> RJob) = withContext(ioDispatcher) {
        synchronized(lock) {
            val job = jobDao.getById(jobID)
                ?: throw SDKException(
                    ErrorCodes.illegal_argument,
                    "Could not find job with ID $jobID"
                )
            val updatedJob = handler(job)
            updatedJob.updateTimestamp = currentTimestamp()
            jobDao.update(updatedJob)
        }
    }

    suspend fun updateTotal(jobID: Long, newTotal: Long, newStatus: String?, message: String?) =
        withContext(ioDispatcher) {
            updateById(jobID) { currJob ->
                currJob.total = newTotal
                newStatus?.let { currJob.status = newStatus }
                message?.let { currJob.progressMessage = message }
                currJob
            }
        }

    suspend fun incrementProgress(jobID: Long, increment: Long, message: String?) =
        withContext(ioDispatcher) {
            updateById(jobID) { currJob ->
                currJob.progress = currJob.progress + increment
                message?.let { currJob.progressMessage = message }
                currJob
            }
        }


    suspend fun launched(jobId: Long) = withContext(ioDispatcher) {
        updateById(jobId) { currJob ->
            currJob.status = JobStatus.PROCESSING.id
            currJob.startTimestamp = currentTimestamp()
            currJob
        }
    }

    suspend fun failed(jobId: Long, errMessage: String) = withContext(ioDispatcher) {
        updateById(jobId) { currJob ->
            currJob.status = JobStatus.ERROR.id
            currJob.doneTimestamp = currentTimestamp()
            currJob.status = errMessage
            currJob
        }
    }

    suspend fun done(jobID: Long, message: String?, lastProgressMsg: String?) =
        updateById(jobID) { currJob ->
            currJob.status = JobStatus.DONE.id
            currJob.doneTimestamp = currentTimestamp()
            currJob.progress = currJob.total
            currJob.message = message
            currJob.progressMessage = lastProgressMsg
            currJob
        }

    suspend fun getRunningJobs(template: String): List<RJob> = withContext(ioDispatcher) {
        return@withContext jobDao.getRunningForTemplate(template)
    }

    suspend fun getRunningChildren(parentID: Long): List<RJob> = withContext(ioDispatcher) {
        return@withContext jobDao.getRunningChildren(parentID)
    }

    suspend fun clearTerminated() = withContext(ioDispatcher) {
        jobDao.clearTerminatedJobs()
    }

    // Logs
    suspend fun clearAllLogs() = withContext(ioDispatcher) {
        logDao.clearLogs()
    }

    suspend fun getLatestRunning(template: String): RJob? = withContext(ioDispatcher) {
        return@withContext jobDao.getLatestRunning(template)
    }

    // Flows and Live Data
    fun getLiveJobByID(jobID: Long): Flow<RJob?> = jobDao.getJobById(jobID)


    fun listLiveJobs(showChildren: Boolean): Flow<List<RJob>> {
        return if (showChildren) {
            jobDao.getLiveJobs()
        } else {
            jobDao.getRootJobs()
        }
    }

    /* MANAGE LOGS */

    fun listLogs(): Flow<List<RLog>> {
        return logDao.getLiveLogs()
    }

    // Shortcut for logging
    fun d(tag: String?, message: String, callerId: String?) {
        Log.d("$tag/JobS.", message + " " + (callerId ?: ""))
        log(AppNames.DEBUG, tag, message, callerId)
    }

    fun i(tag: String?, message: String, callerId: String?) {
        Log.i("$tag/JobS.", "$message - Caller Job ID: ${callerId ?: "-"}")
        log(AppNames.INFO, tag, message, callerId)
    }

    fun w(tag: String?, message: String, callerId: String?) {
        Log.w("$tag/JobS.", message + " " + (callerId ?: ""))
        log(AppNames.WARNING, tag, message, callerId)
    }

    fun e(tag: String?, message: String, callerId: String? = null, e: Exception? = null) {
        log(AppNames.ERROR, tag, message, callerId)
        Log.e("$tag/JobS.", message + " " + (callerId ?: ""))
        e?.printStackTrace()
    }

    private fun log(level: String, tag: String?, message: String, callerId: String?) =
        serviceScope.launch {
            val log = RLog.create(level, tag, message, callerId)
            withContext(ioDispatcher) {
                logDao.insert(log)
            }
        }
}
