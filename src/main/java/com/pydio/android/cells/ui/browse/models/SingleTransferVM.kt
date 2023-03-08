package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID

/** Provides access to a RTransfer record given an account and a transfer ID*/
class SingleTransferVM(
    private val accountID: StateID,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = "SingleTransferVM"

    fun getTransfer(transferID: Long): LiveData<RTransfer?> =
        transferService.liveTransfer(accountID, transferID)

    init {
        Log.d(logTag, "after init for $accountID")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(logTag, "after clear for $accountID")
    }
}
