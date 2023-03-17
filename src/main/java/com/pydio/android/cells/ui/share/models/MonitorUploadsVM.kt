package com.pydio.android.cells.ui.share.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hold a list of file uploads for the given accountID and JobID */
class MonitorUploadsVM(
    val accountID: StateID,
    val jobID: Long,
    prefs: PreferencesService,
    val transferService: TransferService,
) : ViewModel() {

    private val logTag = "MonitorUploadsVM"

    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    val currRecords: LiveData<List<RTransfer>> =
        transferService.getTransfersRecordsForJob(accountID, jobID)

    init {
    }

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(accountID, transferId)
    }

    fun pauseOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.uploadOne(accountID, transferId)
        }
    }

    fun cancelOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun removeOne(transferId: Long) {
        vmScope.launch {
            transferService.deleteRecord(accountID, transferId)
        }
    }

    fun cancelAll() {
        currRecords.value?.forEach {
            try {
                cancelOne(it.transferId)
            } catch (e: Exception) {
                Log.e(logTag, "could not cancel job #$it, cause: ${e.message}")
            }
        }
    }

//    // TODO re-implement support for filter and sort
//    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
//
//    private val liveFilterStatus: MutableLiveData<String> = liveSharedPreferences.getString(
//        AppKeys.JOB_FILTER_BY_STATUS,
//        AppNames.JOB_STATUS_NO_FILTER
//    )
//
//    fun getCurrentTransfers(stateID: StateID): LiveData<List<RTransfer>> {
//        return transferService.queryTransfers(stateID)
//    }

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

}
