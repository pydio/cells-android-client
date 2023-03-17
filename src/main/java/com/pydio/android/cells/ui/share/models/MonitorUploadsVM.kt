package com.pydio.android.cells.ui.share.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Hold a list of file uploads for the given accountID and JobID */
class MonitorUploadsVM(
    val accountID: StateID,
    val jobID: Long,
    val transferService: TransferService,
) : ViewModel() {

    private val logTag = "MonitorUploadsVM"

    val currRecords: LiveData<List<RTransfer>> =
        transferService.getTransfersRecordsForJob(accountID, jobID)

    // TODO add filter and sort

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

    fun cancelAll() {
        currRecords.value?.forEach {
            try {
                cancelOne(it.transferId)
            } catch (e: Exception) {
                Log.e(logTag, "could not cancel job #$it, cause: ${e.message}")
            }
        }
    }

    // Manage UI
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean>
//        get() = _isLoading
//    private val _errorMessage = MutableLiveData<String?>()
//    val errorMessage: LiveData<String?>
//        get() = _errorMessage
//
//    init {}
//
//    override fun onCleared() {
//        super.onCleared()
//    }

}
