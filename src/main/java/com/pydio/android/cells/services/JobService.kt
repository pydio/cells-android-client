package com.pydio.android.cells.services

import android.util.Log
import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.utils.currentTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobService(
//    private val
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
    ): RJob? = withContext(ioDispatcher) {
        val newJob = RJob.create(owner, template, label, parentId)
        newJob.total = maxSteps
        newJob.status = AppNames.JOB_STATUS_PROCESSING
        newJob.startTimestamp = currentTimestamp()
        val jobId = jobDao.insert(newJob)
        return@withContext jobDao.getById(jobId)
    }

    suspend fun incrementProgress(job: RJob, increment: Long, message: String?) =
        withContext(ioDispatcher) {
            job.progress = job.progress + increment
            message?.let { job.progressMessage = message }
            job.updateTimestamp = currentTimestamp()
            jobDao.update(job)
        }

    suspend fun update(job: RJob) = withContext(ioDispatcher) {
        jobDao.update(job)
    }

    suspend fun launched(jobId: Long): String? = withContext(ioDispatcher) {
        val job = jobDao.getById(jobId) ?: return@withContext "Could not find job with ID $jobId"
        job.status = AppNames.JOB_STATUS_PROCESSING
        job.startTimestamp = currentTimestamp()
        job.updateTimestamp = currentTimestamp()
        jobDao.update(job)
        return@withContext null
    }

    suspend fun failed(jobId: Long, errMessage: String): String? = withContext(ioDispatcher) {
        val job = jobDao.getById(jobId) ?: return@withContext "Could not find job with ID $jobId"
        job.status = AppNames.JOB_STATUS_ERROR
        job.doneTimestamp = currentTimestamp()
        job.status = errMessage
        jobDao.update(job)
        return@withContext null
    }

    suspend fun done(job: RJob, message: String?, lastProgressMsg: String?) =
        withContext(ioDispatcher) {
            job.status = AppNames.JOB_STATUS_DONE
            job.doneTimestamp = currentTimestamp()
            job.updateTimestamp = currentTimestamp()
            job.progress = job.total
            job.message = message
            job.progressMessage = lastProgressMsg
            jobDao.update(job)
        }

    suspend fun getRunningJobs(template: String): List<RJob> = withContext(ioDispatcher) {
        return@withContext jobDao.getRunningForTemplate(template)
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


//    fun getMostRecent(template: String): LiveData<RJob?> {
//        return jobDao.getMostRecent(template)
//    }

//     fun getLiveJob(jobId: Long): LiveData<RJob?> = jobDao.getLiveById(jobId)

//    fun getMostRecentRunning(template: String): LiveData<RJob?> {
//        return jobDao.getMostRecentRunning(template)
//    }

    fun listLiveJobs(showChildren: Boolean): LiveData<List<RJob>> {
        return if (showChildren) {
            jobDao.getLiveJobs()
        } else {
            jobDao.getRootJobs()
        }
    }

    /* MANAGE LOGS */

    fun listLogs(): LiveData<List<RLog>> {
        return logDao.getLiveLogs()
    }

    // Shortcut for logging
    fun d(tag: String?, message: String, callerId: String?) {
        Log.d(tag, message + " " + (callerId ?: ""))
        log(AppNames.DEBUG, tag, message, callerId)
    }

    fun i(tag: String?, message: String, callerId: String?) {
        Log.i(tag, "### $message - Caller Job ID: ${callerId ?: "-"}")
        log(AppNames.INFO, tag, message, callerId)
    }

    fun w(tag: String?, message: String, callerId: String?) {
        Log.w(tag, message + " " + (callerId ?: ""))
        log(AppNames.WARNING, tag, message, callerId)
    }

    fun e(tag: String?, message: String, callerId: String? = null, e: Exception? = null) {
        log(AppNames.ERROR, tag, message, callerId)
        Log.e(tag, message + " " + (callerId ?: ""))
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
