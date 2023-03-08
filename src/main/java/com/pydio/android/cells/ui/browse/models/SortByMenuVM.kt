package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences

/** Gives access to the Sort By Preference for the more menu */
class SortByMenuVM(
    private val prefs: CellsPreferences,
) : ViewModel() {

    // private val logTag = "SortByMenuVM"

    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
    val sortBy = livePrefs.getString(
        AppKeys.CURR_RECYCLER_ORDER,
        AppNames.DEFAULT_SORT_BY
    )

    fun setSortBy(newSortBy: String) {
        val doUpdate = sortBy.value != newSortBy
        if (doUpdate) {
            prefs.setString(AppKeys.CURR_RECYCLER_ORDER, newSortBy)
        }
    }
}
