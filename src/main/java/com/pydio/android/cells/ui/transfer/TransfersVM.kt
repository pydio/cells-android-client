package com.pydio.android.cells.ui.transfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hold a list of recent file transfers for current session.
 */
class TransfersVM(
    private val accountService: AccountService,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = TransfersVM::class.simpleName

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView

    val currAccountId: LiveData<StateID?>
        get() = Transformations.map(sessionView) { currSessionView ->
            currSessionView?.accountID?.let { StateID.fromId(it) }
        }

    val currRecords: LiveData<List<RTransfer>>
        get() = Transformations.switchMap(
            sessionView
        ) { currSessionView ->
            val stateID: StateID = currSessionView?.accountID?.let { StateID.fromId(it) }
                ?: Transport.UNDEFINED_STATE_ID
            transferService.queryTransfers(stateID)
        }

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(
            currAccountId.value?.account() ?: Transport.UNDEFINED_STATE_ID,
            transferId
        )
    }

    fun pauseOne(transferId: Long) {
        currAccountId.value?.let {
            viewModelScope.launch {
                // TODO improve this
                transferService.cancelTransfer(it, transferId, AppNames.JOB_OWNER_USER)
            }
        }
    }

    fun resumeOne(transferId: Long) {
        currAccountId.value?.let {
            viewModelScope.launch {
                // TODO improve this
                transferService.uploadOne(it, transferId)
            }
        }
    }

    fun cancelOne(transferId: Long) {
        currAccountId.value?.let {
            viewModelScope.launch {
                transferService.cancelTransfer(it, transferId, AppNames.JOB_OWNER_USER)
            }
        }
    }

    fun removeOne(transferId: Long) {
        currAccountId.value?.let {
            viewModelScope.launch {
                transferService.deleteRecord(it, transferId)
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
//
//    // Manage UI
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean>
//        get() = _isLoading
//    private val _errorMessage = MutableLiveData<String?>()
//    val errorMessage: LiveData<String?>
//        get() = _errorMessage
//
//    private var _oldFilter = prefs.getString(
//        AppKeys.JOB_FILTER_BY_STATUS,
//        AppNames.JOB_STATUS_NO_FILTER
//    )
//    val oldFilter: String
//        get() = _oldFilter
//
//    private var oldSortBy = prefs.getString(
//        AppKeys.TRANSFER_SORT_BY,
//        AppNames.JOB_SORT_BY_DEFAULT
//    )
//
//    override fun onCleared() {
//        super.onCleared()
//        vmJob.cancel()
//    }

}
