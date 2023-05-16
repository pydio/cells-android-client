package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.Flow

/**
 * Exposes the repository while showing an account Home.
 */
class AccountHomeVM(
    val accountID: StateID,
    accountService: AccountService,
) : ViewModel() {

    private val logTag = "AccountHomeVM"

    val currSession: Flow<RSessionView?> = accountService.getLiveSession(accountID)

    val wss: Flow<List<RWorkspace>> =
        accountService.getWsByTypeFlow(SdkNames.WS_TYPE_DEFAULT, accountID.id)

    val cells: Flow<List<RWorkspace>> =
        accountService.getWsByTypeFlow(SdkNames.WS_TYPE_CELL, accountID.id)

    init {
        Log.e(logTag, "Created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "Cleared")
    }
}