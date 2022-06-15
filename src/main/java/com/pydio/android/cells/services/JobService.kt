package com.pydio.android.cells.services

import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.db.runtime.RLog
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobService(runtimeDB: RuntimeDB) {

    private val jobServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + jobServiceJob)

    private val jobDao = runtimeDB.jobDao()
    private val logDao = runtimeDB.logDao()

    fun createAndLaunch(
        owner: String,
        template: String,
        label: String,
        parentId: Long = -1,
        maxSteps: Long = -1
    ): RJob? {
        val newJob = RJob.create(owner, template, label, parentId)
        newJob.total = maxSteps
        newJob.status = AppNames.JOB_STATUS_PROCESSING
        newJob.startTimestamp = currentTimestamp()
        val jobId = jobDao.insert(newJob)
        return jobDao.getById(jobId)
    }

    fun create(
        owner: String,
        template: String,
        label: String,
        parentId: Long = -1,
        maxSteps: Long = -1
    ): Long {
        val newJob = RJob.create(owner, template, label, parentId)
        newJob.total = maxSteps
        newJob.updateTimestamp = currentTimestamp()
        return jobDao.insert(newJob)
    }

    suspend fun launched(jobId: Long): String? = withContext(Dispatchers.IO) {
        val job = jobDao.getById(jobId) ?: return@withContext "Could not find job with ID $jobId"
        job.status = AppNames.JOB_STATUS_PROCESSING
        job.startTimestamp = currentTimestamp()
        job.updateTimestamp = currentTimestamp()

        jobDao.update(job)
        return@withContext null
    }

    suspend fun failed(jobId: Long, errMessage: String): String? = withContext(Dispatchers.IO) {
        val job = jobDao.getById(jobId) ?: return@withContext "Could not find job with ID $jobId"
        job.status = AppNames.JOB_STATUS_ERROR
        job.doneTimestamp = currentTimestamp()
        job.status = errMessage
        jobDao.update(job)
        return@withContext null
    }

    fun get(jobId: Long): RJob? = jobDao.getById(jobId)

    fun getMostRecentRunning(template: String): LiveData<RJob?> {
        return jobDao.getMostRecentRunning(template)
    }

    fun getRunningJobs(template: String): List<RJob> {
        return jobDao.getRunningForTemplate(template)
    }

    fun listLiveJobs(showChildren: Boolean): LiveData<List<RJob>> {
        return if (showChildren) {
            jobDao.getLiveJobs()
        } else {
            jobDao.getRootJobs()
        }
    }

    fun incrementProgress(job: RJob, increment: Long, message: String?) {
        job.progress = job.progress + increment
        message?.let { job.progressMessage = message }
        job.updateTimestamp = currentTimestamp()
        jobDao.update(job)
    }

    fun update(job: RJob) {
        jobDao.update(job)
    }

    fun done(job: RJob, message: String?, lastProgressMsg: String?) {
        job.status = AppNames.JOB_STATUS_DONE
        job.doneTimestamp = currentTimestamp()
        job.updateTimestamp = currentTimestamp()
        job.progress = job.total
        job.message = message
        job.progressMessage = lastProgressMsg
        jobDao.update(job)
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
        Log.i(tag, message + " " + (callerId ?: ""))
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
            withContext(Dispatchers.IO) {
                logDao.insert(log)
            }
        }
}