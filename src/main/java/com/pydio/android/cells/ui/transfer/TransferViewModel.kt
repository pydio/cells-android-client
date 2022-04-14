package com.pydio.android.cells.ui.transfer

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.TransferService

/**
 * Hold a list of recent file transfers.
 */
class TransferViewModel(val transferService: TransferService) : ViewModel() {
    val transfers = transferService.activeTransfers
}
