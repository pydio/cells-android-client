package com.pydio.android.cells.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginHelper(
    private val navController: NavHostController,
    private val loginVM: LoginVM,
    val navigateTo: (String) -> Unit,
//    val startingState: StartingState?,
//    val ackStartStateProcessed: (String?, StateID) -> Unit,
) {
    private val logTag = "LoginHelper"

    fun cancel() {
        navController.popBackStack()
        loginVM.flush()
    }

    suspend fun back() {

        // TODO double check that login navigation is still OK - See below
        loginVM.resetMessages()
        navController.popBackStack()

//        var isFirstLoginPage = true
//        if (bq.size > 1) {
//            val penEntry = bq[bq.size - 2]
//            val penRoute = penEntry.destination.route
//            if (LoginDestinations.isCurrent(penRoute)) {
//                // penultimate route is still in the login subgraph
//                isFirstLoginPage = false
//            }
//        }
//        if (isFirstLoginPage) { // back is then a cancellation of the current login process
//            cancel()
//        } else {
//            loginVM.resetMessages()
//            navController.popBackStack()
//        }
    }

    fun afterPing(res: String) {
        navigateTo(res)
    }

    suspend fun launchP8Auth(
        url: String,
        skipVerify: Boolean,
        loginContext: String,
        login: String,
        pwd: String,
        captcha: String?
    ) {
        val stateID = loginVM.logToP8(url, skipVerify, login, pwd, captcha)
        if (stateID != null) { // Login has been successful, we clean after ourselves and leave the login subgraph
            afterP8Auth(stateID, loginContext)
        } // else do nothing: error message has already been displayed and we stay on the page
    }

    suspend fun launchAuth(
        context: Context,
        stateID: StateID,
        skipVerify: Boolean = false,
        loginContext: String = AuthService.LOGIN_CONTEXT_CREATE
    ) {
        val intent = loginVM.getSessionView(stateID)?.let { sessionView ->
            // Re-authenticating an existing account
            val url = ServerURLImpl.fromAddress(sessionView.url, sessionView.skipVerify())
            loginVM.newOAuthIntent(url, loginContext)
        } ?: run {
            Log.i(logTag, "Launching OAuth Process for new account $stateID")
            val url = ServerURLImpl.fromAddress(stateID.serverUrl, skipVerify)
            loginVM.newOAuthIntent(url, loginContext)
        }
        intent?.let {
            withContext(Dispatchers.Main) {
                ContextCompat.startActivity(context, intent, null)
            }
            navController.popBackStack()
        }
    }

    //    suspend fun processAuth(stateID: StateID) {
//
//        if (startingState == null || !LoginDestinations.ProcessAuthCallback.isCurrent(startingState.route)) {
//            Log.e(logTag, "## In processAuth for state: $stateID")
//            Log.e(logTag, "##  invalid starting state or route: ${startingState?.route}")
//            Thread.dumpStack()
//            return
//        }
//
//        Log.i(logTag, "... In processAuth for: $stateID")
//        Log.d(logTag, "     route: ${startingState.route}")
//        Log.d(logTag, "     OAuth state: ${startingState.state}")
//
//        loginVM.handleOAuthResponse(
//            // We assume nullity has already been checked
//            state = startingState.state!!,
//            code = startingState.code!!,
//        )?.let {
//            Log.i(logTag, "    -> OAuth OK, login context: ${it.second}")
//            afterAuth(it.first, it.second)
//        } ?: run {
//            // TODO better error handling
//            ackStartStateProcessed(
//                null,
//                StateID.NONE
//            )
//        }
//    }
//
//
    private fun afterP8Auth(stateID: StateID, loginContext: String?) {
//         ackStartStateProcessed(null, stateID)

        Log.e(logTag, "... After OAuth: $stateID, context: $loginContext, unstacking destinations:")

        // FIXME remove
        val bseList = navController.currentBackStack.value
        Log.e(logTag, "... Looping back stack")
        var i = 1
        for (bse in bseList) {
            Log.e(logTag, " #$i: ${bse.destination.route}")
            i++
        }
        Log.e(logTag, "... Looping done")

        var stillLogin = true
        while (stillLogin) {
            val tmp = navController.currentBackStackEntry
            Log.e(logTag, " - curr dest: ${tmp?.destination?.route}")
            tmp?.let {
                if (LoginDestinations.isCurrent(it.destination.route)) {
                    navController.popBackStack()
                } else {
                    stillLogin = false
                }
            } ?: run { stillLogin = false }
        }

        if (loginContext == AuthService.LOGIN_CONTEXT_CREATE) {
            // New account -> we open it
            navigateTo(BrowseDestinations.Open.createRoute(stateID))
        } else {
            // We only get rid of login pages.
        }
        loginVM.flush()
    }
}
