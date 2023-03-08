package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

/** Holds a list of recent file transfers for current session */
class TransfersVM(
    private val accountID: StateID,
    private val prefs: CellsPreferences,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = "TransfersVM"

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
    private val filterBy = livePrefs.getString(
        AppKeys.JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )

    val transfers: LiveData<List<RTransfer>>
        get() = Transformations.switchMap(
            filterBy
        ) { currFilter ->
            transferService.queryTransfersExplicitFilter(accountID, currFilter)
        }

    suspend fun get(transferID: Long): RTransfer? =
        transferService.getRecord(accountID, transferID)


    fun pauseOne(transferID: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferID, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferID: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.uploadOne(accountID, transferID)
        }
    }

    fun cancelOne(transferID: Long) {
        viewModelScope.launch {
            transferService.cancelTransfer(accountID, transferID, AppNames.JOB_OWNER_USER)
        }
    }

    fun removeOne(transferID: Long) {
        Log.i(logTag, "About to delete $transferID @ $accountID")
        viewModelScope.launch {
            transferService.deleteRecord(accountID, transferID)
        }
    }
}
