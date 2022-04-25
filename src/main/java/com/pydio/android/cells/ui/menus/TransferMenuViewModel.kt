package com.pydio.android.cells.ui.menus

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID

/**
 * Holds a Transfer record for the dedicated context menu.
 */
class TransferMenuViewModel(
    accountId: String,
    transferUID: Long,
    val transferService: TransferService
) : ViewModel() {

//    private val tag = TransferMenuViewModel::class.simpleName

    val rTransfer = transferService.getLiveRecord(StateID.fromId(accountId), transferUID)

}