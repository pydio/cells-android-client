package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.services.PreferencesService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Gives access to the Sort By Preference for the more menu */
class SortByMenuVM(
    private val type: ListType,
    private val prefs: PreferencesService,
) : ViewModel() {

    val encodedOrder = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        when (type) {
            ListType.TRANSFER -> cellsPreferences.list.transferOrder
            ListType.JOB -> cellsPreferences.list.jobOrder
            ListType.DEFAULT -> cellsPreferences.list.order
        }
    }

    fun setSortBy(newSortBy: String) {
        viewModelScope.launch {
            prefs.setOrder(type, newSortBy)
        }
    }
}
