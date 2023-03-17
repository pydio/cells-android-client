package com.pydio.android.cells.ui.system.models

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.core.ListLayout
import kotlinx.coroutines.launch

/** Expose methods used to perform house keeping on the App */
class SettingsVM(
    private val prefs: PreferencesService,
    // private val nodeService: NodeService
) : ViewModel() {


    val cellsPreferences = prefs.cellsPreferencesFlow

    fun setShowRuntimeToolsFlag(show: Boolean) {
        viewModelScope.launch {
            prefs.setShowDebugToolsFlag(show)
        }
    }

    fun setDefaultOrder(order: String) {
        viewModelScope.launch {
            prefs.setOrder(ListType.DEFAULT, order)
        }
    }

    fun setListLayout(layoutStr: String) {
        val layout: ListLayout = if (ListLayout.GRID.name == layoutStr)
            ListLayout.GRID
        else
            ListLayout.LIST
        viewModelScope.launch {
            prefs.setListLayout(layout)
        }
    }

    fun setBooleanFlag(key: Preferences.Key<Boolean>, flag: Boolean) {
        viewModelScope.launch {
            prefs.setBoolean(key, flag)
        }
    }

    fun setStringPref(key: Preferences.Key<String>, strValue: String) {
        viewModelScope.launch {
            prefs.setString(key, strValue)
        }
    }
}
