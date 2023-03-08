package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.ui.core.ListLayout

/** Expose methods used to perform house keeping on the App */
class SettingsVM(
    private val prefs: CellsPreferences,
    // private val nodeService: NodeService
) : ViewModel() {

    // private val logTag = "SettingsVM"

    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
    private val sortOrder = livePrefs.getString(
        AppKeys.CURR_RECYCLER_ORDER,
        AppNames.DEFAULT_SORT_BY
    )
    val layout = livePrefs.getLayout(AppKeys.CURR_RECYCLER_LAYOUT, ListLayout.LIST)

    fun setSortBy(newSortBy: String) {
        val doUpdate = sortOrder.value != newSortBy
        if (doUpdate) {
            prefs.setString(AppKeys.CURR_RECYCLER_ORDER, newSortBy)
        }
    }
}
