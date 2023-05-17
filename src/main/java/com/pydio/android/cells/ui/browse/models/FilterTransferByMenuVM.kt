package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.PreferencesKeys
import com.pydio.android.cells.services.PreferencesService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Gives access to the "Filter Transfer By Status" Preference for the transfers more menu */
class FilterTransferByMenuVM(
    private val prefs: PreferencesService,
) : ViewModel() {

    private val logTag = "FilterTransferByMenuVM"

    val transferFilter = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.transferFilter
    }

    fun setFilterBy(newFilterByStatus: String) {
        viewModelScope.launch {
            prefs.setString(PreferencesKeys.TRANSFER_FILTER_BY_STATUS, newFilterByStatus)
        }
    }
}
