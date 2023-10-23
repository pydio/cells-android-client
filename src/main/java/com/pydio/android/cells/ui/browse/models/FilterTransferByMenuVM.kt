package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.PreferencesKeys
import com.pydio.android.cells.services.PreferencesService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Gives access to the "Filter Transfer By Status" Preference for the transfer more menu */
class FilterTransferByMenuVM(
    private val prefs: PreferencesService,
) : ViewModel() {

    private val logTag = "FilterTransferByMenuVM"

    val transferFilter = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.transferFilter
    }

    fun setFilterBy(newFilterByStatus: String) {
        viewModelScope.launch {
            try {
                prefs.setString(PreferencesKeys.TRANSFER_FILTER_BY_STATUS, newFilterByStatus)
            } catch (e: Exception) {
                Log.e(logTag, "Could not update filter by status preference: ${e.message}")
                // TODO forward to the end user.
            }
        }
    }
}
