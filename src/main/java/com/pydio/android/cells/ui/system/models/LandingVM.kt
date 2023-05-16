package com.pydio.android.cells.ui.system.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
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
    private val jobService: JobService,
    private val authService: AuthService,
    private val accountService: AccountService,
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

    suspend fun isAuthStateValid(state: String): Pair<Boolean, StateID> {
        return authService.isAuthStateValid(state)
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

        // probably useless, TODO double check
        // stateID?.let { accountService.openSession(it.account()) }

        val state = StartingState(stateID ?: StateID.NONE)
        val route = when (stateID) {
            null -> LoginDestinations.AskUrl.createRoute()
            StateID.NONE -> CellsDestinations.Accounts.route
            else -> {
                // We are most probably in a restart, so we prevent explicit browsing
                // FIXME Do we really still need this ?
                state.isRestart = true

                BrowseDestinations.Open.createRoute(stateID)
            }
        }
        state.route = route
        return state
    }

    fun recordLaunch() {
        try {
            val creationMsg = "### Started ${ClientData.getInstance().userAgent()}"
            jobService.i(logTag, creationMsg, "Cells App")
//            jobService.d(logTag, ".... Testing log levels:", "DEBUGGER")
//            jobService.i(logTag, "   check - 1", "DEBUGGER")
//            jobService.w(logTag, "   check - 2. with a very very very very very very, very very very very long message!!!!!!!", "DEBUGGER")
//            jobService.e(logTag, "   check - 3", "DEBUGGER")
        } catch (e: Exception) {
            Log.e(logTag, "could not log start: $e")
        }
    }
}
