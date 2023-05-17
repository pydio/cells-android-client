package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds a list of recent file transfers for current session */
class TransfersVM(
    private val accountID: StateID,
    private val transferService: TransferService,
) : AbstractCellsVM() {

    private val logTag = "TransfersVM"


    private val transferOrderPair = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.TRANSFER
        )
    }

    val liveFilter: Flow<String> = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.transferFilter
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transfers: Flow<List<RTransfer>> = transferOrderPair.flatMapLatest { currPair ->
        transferService.queryTransfersExplicitFilter(
            accountID,
            currPair.first,
            currPair.second
        )
    }

    suspend fun get(transferID: Long): RTransfer? = transferService.getRecord(accountID, transferID)

    fun pauseOne(transferID: Long) {
        viewModelScope.launch {
            // TODO improve this
            transferService.cancelTransfer(accountID, transferID, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferID: Long) {
        CellsApp.instance.appScope.launch {
//        viewModelScope.launch {
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

    fun clearTerminated() {
        Log.i(logTag, "About to empty transfer table for $accountID")
        viewModelScope.launch {
            transferService.clearTerminated(accountID)
        }
    }

    fun forceRefresh() {
        // DO nothing
    }


    init {
        // We are always "idle" in this view
        done()
    }
}
