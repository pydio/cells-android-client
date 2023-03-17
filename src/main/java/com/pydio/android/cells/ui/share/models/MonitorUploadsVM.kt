package com.pydio.android.cells.ui.share.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
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
    prefs: PreferencesService,
    val transferService: TransferService,
) : ViewModel() {

    private val logTag = "MonitorUploadsVM"

    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    private lateinit var _accountID: StateID
    private var _jobID: Long = 0L

    private lateinit var _currRecords: LiveData<List<RTransfer>>
    val currRecords: LiveData<List<RTransfer>>
        get() = _currRecords

    fun afterCreate(accountID: StateID, jobID: Long) {
        _accountID = accountID
        _jobID = jobID
        _currRecords = transferService.getTransfersRecordsForJob(accountID, jobID)
    }

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(_accountID, transferId)
    }

    fun pauseOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(_accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.uploadOne(_accountID, transferId)
        }
    }

    fun cancelOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(_accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun removeOne(transferId: Long) {
        vmScope.launch {
            transferService.deleteRecord(_accountID, transferId)
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

    private var _oldFilter = prefs.getString(
        AppKeys.JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )
    val oldFilter: String
        get() = _oldFilter

    private var oldSortBy = prefs.getString(
        AppKeys.TRANSFER_SORT_BY,
        AppNames.JOB_SORT_BY_DEFAULT
    )

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }


//    // TODO rather retrieve on the properties than on the full object
//    open inner class ItemState (
//        private val rTransfer: RTransfer,
//    )  {
//
//        fun isFailed(): Boolean {
//            return Str.notEmpty(rTransfer.error)
//        }
//
//        fun isDone(): Boolean {
//            return rTransfer.doneTimestamp > 0
//        }
//
//        val ids: MutableMap<Long, Pair<String, Uri>> = HashMap()
//
//    }

    inner class UploadState(
        val transferId: Long,
        val uri: Uri,
        val fname: String,
    ) {
        private var _status: String = AppNames.JOB_STATUS_NEW
        val status: String
            get() = _status

        fun updateStatus(newStatus: String) {
            _status = newStatus
        }
    }


//    private fun reQuery(value: String) {
//        liveFilterStatus.value = value
//    }
//
//    init {
//        // reQuery(_oldFilter)
//
//        vmScope.launch {
//            liveSharedPreferences.getString(
//                AppKeys.TRANSFER_FILTER_BY_STATUS,
//                AppNames.JOB_STATUS_NO_FILTER
//            ).asFlow().collect {
//                // it?.let {
//                if (it != _oldFilter) {
//                    _oldFilter = it
//                    reQuery(it)
//                } else { // this should never happen
//                    Log.w(logTag, "Received a new event for same query, this should not happen")
//                }
//                // }
//            }
//            liveSharedPreferences.getString(
//                AppKeys.TRANSFER_SORT_BY,
//                AppNames.JOB_SORT_BY_DEFAULT
//            ).asFlow().collect {
//                //it?.let {
//                if (it != oldSortBy) {
//                    oldSortBy = it
//                    reQuery(it)
//                }
//                // }
//            }
//        }
//    }
}
