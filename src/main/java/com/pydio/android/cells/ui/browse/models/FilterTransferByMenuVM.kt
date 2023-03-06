package com.pydio.android.cells.ui.browse.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences

/** Gives access to the "Filter Transfer By Status" Preference for the transfers more menu */
class FilterTransferByMenuVM(
    private val prefs: CellsPreferences,
) : ViewModel() {

    // private val logTag = FilterTransferByMenuVM::class.simpleName

    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
    val filterBy = livePrefs.getString(
        AppKeys.JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )

    fun setFilterBy(newFilterByStatus: String) {
        val doUpdate = filterBy.value != newFilterByStatus
        if (doUpdate) {
            prefs.setString(AppKeys.JOB_FILTER_BY_STATUS, newFilterByStatus)
        }
    }
}
