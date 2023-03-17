package com.pydio.android.cells.ui.system.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class LandingVM(
    private val prefs: PreferencesService,
    private val accountService: AccountService,
    private val jobService: JobService,
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
        Log.d(logTag, "About to clear")
    }

    /**
     * Makes a first quick check for the happy path and returns true if:
     * - code version is the same as the stored version
     * OR (TODO still checked during migrate activity for the time being)
     * - we are on a fresh install
     *
     * WARNING: false mean that we have to trigger the migrate activity that will perform
     * more advanced tests, do a migration (if really necessary) and update the stored version number.
     */
    suspend fun noMigrationNeeded(): Boolean {
        val currInstalled = prefs.getInstalledVersion()
        // TODO find a way to know if we are on a fresh install
        return newVersion > 100 && newVersion == currInstalled
    }

    suspend fun getStartingState(): StartingState {

        var stateID: StateID?
        // TODO get latest known state from preferences and navigate to it

        // Fallback on defined accounts:
        val sessions = accountService.listSessionViews(true)
        stateID = when (sessions.size) {
            0 -> null
            1 -> sessions[0].getStateID()
            else -> {
                // If a session is listed as in foreground, we open this one
                accountService.getActiveSession()?.getStateID() ?: StateID.NONE
            }
        }

        val route = when (stateID) {
            null -> LoginDestinations.AskUrl.createRoute()
            StateID.NONE -> CellsDestinations.Accounts.route
            else -> BrowseDestinations.Open.createRoute(stateID)
        }

        // probably useless, TODO double check
        stateID?.let { accountService.openSession(it.account()) }

        val state = StartingState(stateID ?: StateID.NONE)
        state.route = route
        return state
    }

    fun recordLaunch() {
        try {
            val creationMsg = "### Starting agent ${ClientData.getInstance().userAgent()}"
            jobService.i(logTag, creationMsg, "Cells App")
//            jobService.d(logTag, ".... Testing log levels:", "DEBUGGER")
//            jobService.i(logTag, "   check - 1", "DEBUGGER")
//            jobService.w(logTag, "   check - 2. with a very very very very very very, very very very very looooong message!!!!!!!", "DEBUGGER")
//            jobService.e(logTag, "   check - 3", "DEBUGGER")
        } catch (e: Exception) {
            Log.e(logTag, "could not log start: $e")
        }
    }

}
