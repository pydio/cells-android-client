package com.pydio.android.cells.services

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
        label: String,
        template: String,
        parentId: Long = 0,
        maxSteps: Long = -1
    ): RJob? {
        val newJob = RJob.create(label, template, parentId)
        newJob.progressMax = maxSteps
        newJob.status = AppNames.JOB_STATUS_PROCESSING
        newJob.startTimestamp = currentTimestamp()
        val jobId = jobDao.insert(newJob)
        return jobDao.getById(jobId)
    }

    fun update(job: RJob, increment: Long, message: String?) {
        job.progress = job.progress + increment
        message?.let { job.progressMessage = message }
        jobDao.update(job)
    }

    fun done(job: RJob, message: String?) {
        job.status = AppNames.JOB_STATUS_DONE
        job.doneTimestamp = currentTimestamp()
        job.progress = job.progressMax
        job.progressMessage = message
        jobDao.update(job)
    }

    // Shortcut for logging
    fun i(tag: String?, message: String, callerId: String?) {
        Log.i(tag, message + " " + (callerId ?: ""))
        log(AppNames.INFO, tag, message, callerId)
    }

    fun e(tag: String?, message: String, callerId: String? = null) {
        Log.e(tag, message + " " + (callerId ?: ""))
        log(AppNames.ERROR, tag, message, callerId)
    }

    private fun log(level: String, tag: String?, message: String, callerId: String?) =
        serviceScope.launch {
            val log = RLog.create(level, tag, message, callerId)
            withContext(Dispatchers.IO) {
                logDao.insert(log)
            }
        }
}