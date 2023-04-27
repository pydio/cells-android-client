package com.pydio.android.cells.ui.browse.models

import android.util.Log
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

    val jobFilter = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.jobFilter
    }

    fun setFilterBy(newFilterByStatus: String) {
        viewModelScope.launch {
            Log.e(logTag, "Got a new filter: $newFilterByStatus")
            prefs.setString(PreferencesKeys.TRANSFER_FILTER_BY_STATUS, newFilterByStatus)
        }
    }
}
