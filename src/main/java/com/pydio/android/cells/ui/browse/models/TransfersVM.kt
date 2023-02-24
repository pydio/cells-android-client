package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds a list of recent file transfers for current session */
class TransfersVM(
    private val accountService: AccountService,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = TransfersVM::class.simpleName

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    // TODO re-implement support for filter and sort

//    val sessionView: LiveData<RSessionView?> = accountService.liveActiveSessionView
//    val currAccountId: LiveData<StateID?>
//        get() = Transformations.map(sessionView) { currSessionView ->
//            currSessionView?.accountID?.let { StateID.fromId(it) }
//        }
//val currRecords: LiveData<List<RTransfer>>
//    get() = Transformations.switchMap(
//        sessionView
//    ) { currSessionView ->
//        val stateID: StateID = currSessionView?.accountID?.let { StateID.fromId(it) }
//            ?: Transport.UNDEFINED_STATE_ID
//        transferService.queryTransfers(stateID)
//    }

    private val accountID: MutableLiveData<StateID> = MutableLiveData(Transport.UNDEFINED_STATE_ID)
    val transfers: LiveData<List<RTransfer>>
        get() = Transformations.switchMap(
            accountID
        ) { currID ->
            if (currID == Transport.UNDEFINED_STATE_ID) {
                MutableLiveData()
            } else {
                transferService.queryTransfers(currID)
            }
        }

    fun afterCreate(accountID: StateID) {
        this.accountID.value = accountID
    }

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(
            accountID.value?.account() ?: Transport.UNDEFINED_STATE_ID,
            transferId
        )
    }

    fun pauseOne(transferId: Long) {
        accountID.value?.let {
            viewModelScope.launch {
                // TODO improve this
                transferService.cancelTransfer(it, transferId, AppNames.JOB_OWNER_USER)
            }
        }
    }

    fun resumeOne(transferId: Long) {
        accountID.value?.let {
            viewModelScope.launch {
                // TODO improve this
                transferService.uploadOne(it, transferId)
            }
        }
    }

    fun cancelOne(transferId: Long) {
        accountID.value?.let {
            viewModelScope.launch {
                transferService.cancelTransfer(it, transferId, AppNames.JOB_OWNER_USER)
            }
        }
    }

    fun removeOne(transferId: Long) {
        accountID.value?.let {
            viewModelScope.launch {
                transferService.deleteRecord(it, transferId)
            }
        }
    }
}
