package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.PreferencesService
import kotlinx.coroutines.flow.map

/** Expose current state of the preferences as cold flows for the various views */
class PrefReadOnlyVM(
    prefs: PreferencesService,
) : ViewModel() {

    private val cellsPreferences = prefs.cellsPreferencesFlow

    val showDebugTools = cellsPreferences.map { prefs -> prefs.showDebugTools }
}
