package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds a list of recent file transfers for current session */
class TransfersVM(
    legacy: Boolean,
    private val accountID: StateID,
    private val transferService: TransferService,
) : AbstractCellsVM() {

    private val logTag = "TransfersVM"

    private val transferOrder = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.transferOrder
    }

    val liveFilter: Flow<String> = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.transferFilter
    }

    val isRemoteServerLegacy = legacy

    @OptIn(ExperimentalCoroutinesApi::class)
    val transfers: Flow<List<RTransfer>> =
        liveFilter.combine(transferOrder) { filter, order -> filter to order }
            .flatMapLatest { pair ->
                transferService.queryTransfersExplicitFilter(
                    accountID,
                    pair.first,
                    pair.second
                )
            }

    suspend fun get(transferID: Long): RTransfer? = transferService.getRecord(accountID, transferID)

    fun pauseOne(transferID: Long) {
        viewModelScope.launch {
            try {
                if (isRemoteServerLegacy) {
                    error("Cannot pause transfer when remote server is Pydio 8")
                } else {
                    transferService.pauseTransfer(
                        accountID,
                        transferID,
                        AppNames.JOB_OWNER_USER,
                        false
                    )
                }
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun resumeOne(transferID: Long) {
        viewModelScope.launch {
            try {
                if (isRemoteServerLegacy) {
                    error("Cannot resume transfer when remote server is Pydio 8")
                } else {
                    transferService.resumeTransfer(
                        accountID,
                        transferID,
                        false
                    )
                }
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun cancelOne(transferID: Long) {
        viewModelScope.launch {
            try {
                transferService.cancelTransfer(
                    accountID,
                    transferID,
                    AppNames.JOB_OWNER_USER,
                    isRemoteServerLegacy
                )

            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun removeOne(transferID: Long) {
        Log.i(logTag, "About to delete $transferID @ $accountID")
        viewModelScope.launch {
            try {
                transferService.forgetTransfer(accountID, transferID, isRemoteServerLegacy)
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun clearTerminated() {
        Log.i(logTag, "About to empty transfer table for $accountID")
        viewModelScope.launch {
            // TODO better management of terminated transfers
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
