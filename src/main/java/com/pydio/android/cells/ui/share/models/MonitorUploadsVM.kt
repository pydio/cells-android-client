package com.pydio.android.cells.ui.share.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hold a list of file uploads for the given accountID and JobID */
class MonitorUploadsVM(
    val accountID: StateID,
    val jobID: Long,
    val jobService: JobService,
    val transferService: TransferService,
) : ViewModel() {

    // private val logTag = "MonitorUploadsVM"

    val parentJob: Flow<RJob?> = jobService.getLiveJobByID(jobID)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currRecords: Flow<List<RTransfer>> =
        transferService.getChildTransfersRecords(accountID, jobID)

    fun getStatusFromListAndJob(job: RJob?, rTransfers: List<RTransfer>): JobStatus {
        var newStatus = JobStatus.NEW
        job?.let {
            if (it.isFail()) {
                newStatus = JobStatus.ERROR
            } else {
                var hasRunning = false
                for (rTransfer in rTransfers) {
                    if (rTransfer.status != AppNames.JOB_STATUS_DONE) {
                        hasRunning = true
                        break
                    }
                }
                newStatus = if (hasRunning) {
                    JobStatus.PROCESSING
                } else {
                    // TODO we update the Parent job from here. Improve this
                    markJobAsDone(job)
                    JobStatus.DONE
                }
            }
        }
        return newStatus
    }

    private fun markJobAsDone(job: RJob) {
        viewModelScope.launch {
            if (!job.isDone()) {
                jobService.done(job, "All files have been uploaded", null)
            }
        }
    }

    // Also add filter and sort ??

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(accountID, transferId)
    }

    fun pauseOne(transferId: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferId: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.uploadOne(accountID, transferId)
        }
    }

    fun cancelOne(transferId: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun removeOne(transferId: Long) {
        viewModelScope.launch {
            transferService.deleteRecord(accountID, transferId)
        }
    }

//    fun cancelAll() {
//        currRecords.value?.forEach {
//            try {
//                cancelOne(it.transferId)
//            } catch (e: Exception) {
//                Log.e(logTag, "could not cancel job #$it, cause: ${e.message}")
//            }
//        }
//    }
}
