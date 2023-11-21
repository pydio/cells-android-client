package com.pydio.android.cells.ui.system.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.PreferencesService
import com.pydio.cells.transport.ClientData
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class LandingVM(
    private val prefs: PreferencesService,
//    private val jobService: JobService,
//    private val authService: AuthService,
//    private val accountService: AccountService,
) : ViewModel() {

    private val logTag = "LandingVM"
    private var oldVersion by Delegates.notNull<Int>()
    private val newVersion = ClientData.getInstance().versionCode.toInt()

    init {
        viewModelScope.launch {
            oldVersion = prefs.getInstalledVersion()
        }
    }

    override fun onCleared() {
        // useless: this does nothing
        // super.onCleared()
        Log.d(logTag, "... Cleared")
    }

    /**
     * Makes a first quick check for the happy path and returns true
     * only if code version is the same as the stored version
     *
     * Note: "false" means that we have to trigger the migrate activity that:
     * - performs advanced tests,
     * - does a migration (if necessary)
     * - updates the stored version number.
     *
     * Note: We also get "false" for fresh installs and go through migration.
     *  It takes a few seconds more to start but  subsequent starts are then faster:
     *  they avoid instantiating legacy migration objects
     */
    suspend fun noMigrationNeeded(): Boolean {
        val currInstalled = prefs.getInstalledVersion()
        return newVersion > 100 && newVersion == currInstalled
    }

//    suspend fun isAuthStateValid(state: String): Pair<Boolean, StateID> {
//        return authService.isAuthStateValid(state)
//    }

}
