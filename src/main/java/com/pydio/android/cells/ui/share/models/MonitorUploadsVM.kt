package com.pydio.android.cells.ui.share.models

import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Hold a list of file uploads for the given accountID and JobID */
class MonitorUploadsVM(
    val isRemoteLegacy: Boolean,
    val accountID: StateID,
    val jobID: Long,
    val jobService: JobService,
    val transferService: TransferService,
) : AbstractCellsVM() {

    // private val logTag = "MonitorUploadsVM"

    val parentJob: Flow<RJob?> = jobService.getLiveJobByID(jobID)

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
                    if (JobStatus.DONE.id != rTransfer.status) {
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

    suspend fun get(transferId: Long): RTransfer? {
        return transferService.getRecord(accountID, transferId)
    }

    fun pauseOne(transferID: Long) {
        viewModelScope.launch {
            try {
                if (isRemoteLegacy) {
                    error("Cannot pause transfer when remote server is Pydio 8")
                } else {
                    transferService.pauseTransfer(
                        accountID,
                        transferID,
                        AppNames.JOB_OWNER_USER,
                        false
                    )
                }
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun resumeOne(transferID: Long) {
        viewModelScope.launch {
            try {
                if (isRemoteLegacy) {
                    error("Cannot resume transfer when remote server is Pydio 8")
                } else {
                    transferService.resumeTransfer(
                        accountID,
                        transferID,
                        false
                    )
                }
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun cancelOne(transferID: Long) {
        viewModelScope.launch {
            try {
                transferService.cancelTransfer(
                    accountID,
                    transferID,
                    AppNames.JOB_OWNER_USER,
                    isRemoteLegacy
                )

            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun removeOne(transferID: Long) {
        viewModelScope.launch {
            try {
                transferService.forgetTransfer(accountID, transferID, isRemoteLegacy)
            } catch (e: Exception) {
                done(e)
            }
        }
    }
}
