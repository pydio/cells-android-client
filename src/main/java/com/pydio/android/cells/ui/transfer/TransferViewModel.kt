package com.pydio.android.cells.ui.transfer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID

/**
 * Hold a list of recent file transfers for current session.
 */
class TransferViewModel(
    val state: String?,
    val transferService: TransferService
) : ViewModel() {
    val transfers = state?.let { transferService.activeTransfers(StateID.fromId(state)) }
        ?: MutableLiveData<List<RTransfer>?>()
}
