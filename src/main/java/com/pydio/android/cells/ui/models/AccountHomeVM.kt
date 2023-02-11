package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.services.AccountService
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val logTag = AccountHomeVM::class.simpleName

/**
 * Exposes the repository while showing an account Home.
 */
class AccountHomeVM(
    private val accountService: AccountService,
) : ViewModel() {

    private val _accountID = MutableStateFlow(Transport.UNDEFINED_STATE_ID)
    val accountID: StateFlow<StateID> = _accountID.asStateFlow()

    private lateinit var _wss: LiveData<List<RWorkspace>>
    val wss: LiveData<List<RWorkspace>>
        get() = _wss

    private lateinit var _cells: LiveData<List<RWorkspace>>
    val cells: LiveData<List<RWorkspace>>
        get() = _cells

    fun setState(accountID: StateID) {
        Log.e(logTag, "--- Updating current state to $accountID")
        _accountID.value = accountID
        _wss = accountService.getLiveWsByType(SdkNames.WS_TYPE_DEFAULT, accountID.id)
        _cells = accountService.getLiveWsByType(SdkNames.WS_TYPE_CELL, accountID.id)
    }

    init {
        Log.e(logTag, "--- Created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.e(logTag, "--- Cleared")
    }
}
