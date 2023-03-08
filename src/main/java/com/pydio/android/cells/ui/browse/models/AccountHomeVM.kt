package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.StateID

/**
 * Exposes the repository while showing an account Home.
 */
class AccountHomeVM(
    val accountID: StateID,
    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = "AccountHomeVM"

    val wss: LiveData<List<RWorkspace>>
        get() = accountService.getLiveWsByType(SdkNames.WS_TYPE_DEFAULT, accountID.id)

    val cells: LiveData<List<RWorkspace>>
        get() = accountService.getLiveWsByType(SdkNames.WS_TYPE_CELL, accountID.id)

    val currSession: LiveData<RSessionView?>
        get() = accountService.getLiveSession(accountID)

    init {
        Log.e(logTag, "--- Created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "--- Cleared")
    }
}