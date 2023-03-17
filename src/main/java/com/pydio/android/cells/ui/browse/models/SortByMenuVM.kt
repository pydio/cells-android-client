package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.services.PreferencesService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Gives access to the Sort By Preference for the more menu */
class SortByMenuVM(
    private val prefs: PreferencesService,
) : ViewModel() {

    // private val logTag = "SortByMenuVM"
//
//    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
//    val sortBy = livePrefs.getString(
//        AppKeys.CURR_RECYCLER_ORDER,
//        AppNames.DEFAULT_SORT_BY
//    )

    val encodedOrder = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.order
    }

    fun setSortBy(newSortBy: String) {
        viewModelScope.launch {
            prefs.setOrder(ListType.DEFAULT, newSortBy)
        }
    }
}
