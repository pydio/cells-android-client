package com.pydio.android.cells.ui.transfer

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.AccountService

/**
 * Holds a list of connected clients to choose a target destination for uploads and moves.
 */
class PickSessionViewModel(accountService: AccountService) : ViewModel() {
    val sessions = accountService.liveSessionViews
}
