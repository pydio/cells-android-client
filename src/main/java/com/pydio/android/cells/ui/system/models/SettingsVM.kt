package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.PreferencesService
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
}
