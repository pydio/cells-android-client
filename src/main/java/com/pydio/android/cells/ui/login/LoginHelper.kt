package com.pydio.android.cells.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.StartingState
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
    val startingState: StartingState?,
    val ackStartStateProcessed: (String?, StateID) -> Unit,
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
        login: String,
        pwd: String,
        captcha: String?
    ) {
        val stateID = loginVM.logToP8(url, skipVerify, login, pwd, captcha)
        if (stateID != null) {
            // Login has been successful,
            // We clean after ourselves and leave the login subgraph
            afterAuth(stateID, AuthService.NEXT_ACTION_BROWSE)
        } // else do nothing: error message has already been displayed and we stay on the page
    }

    suspend fun launchAuth(
        context: Context,
        stateID: StateID,
        skipVerify: Boolean = false,
        nextAction: String = AuthService.NEXT_ACTION_BROWSE
    ) {
        val intent = loginVM.getSessionView(stateID)?.let { sessionView ->
            // Re-authenticating an existing account
            val url = ServerURLImpl.fromAddress(sessionView.url, sessionView.skipVerify())
            loginVM.newOAuthIntent(url, nextAction)
        } ?: run {
            Log.i(logTag, "Launching OAuth Process for new account $stateID")
            val url = ServerURLImpl.fromAddress(stateID.serverUrl, skipVerify)
            loginVM.newOAuthIntent(url, nextAction)
        }
        intent?.let {
            withContext(Dispatchers.Main) {
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

    suspend fun processAuth(stateID: StateID) {

        if (startingState == null || !LoginDestinations.ProcessAuthCallback.isCurrent(startingState.route)) {
            Log.e(logTag, "## In processAuth for state: $stateID")
            Log.e(logTag, "##  invalid starting state or route: ${startingState?.route}")
            Thread.dumpStack()
            return
        }

        Log.i(logTag, "## In processAuth for: $stateID")
        Log.d(logTag, "##    route: ${startingState.route}")
        Log.d(logTag, "##    OAuth state: ${startingState.state}")

        loginVM.handleOAuthResponse(
            // We assume nullity has already been checked
            state = startingState.state!!,
            code = startingState.code!!,
        )?.let {
            Log.i(logTag, "OAuth OK - ${it.first}")
            afterAuth(it.first, it.second)
        } ?: run {
            // TODO better error handling
            ackStartStateProcessed(
                null,
                StateID.NONE
            )
        }

//        when {
//            startingState != null && LoginDestinations.ProcessAuth.isCurrent(startingState.route)
//            -> { // OAuth flow Callback
//                Log.d(logTag, "Process OAuth response for $stateID and ${startingState.state}")
//            }
//
//            stateID != StateID.NONE
//            -> { // The user wants to login again in an expired already registered account
//                // FIXME implement next
//                val nextAction = AuthService.NEXT_ACTION_BROWSE
//                loginVM.getSessionView(stateID)?.let { sessionView ->
//                    val url = ServerURLImpl.fromAddress(sessionView.url, sessionView.skipVerify())
//                    val intent = loginVM.newOAuthIntent(url, nextAction)
//                    intent?.let {
//                        withContext(Dispatchers.Main) {
//                            ContextCompat.startActivity(context, intent, null)
//                        }
//                    }
//                } ?: run {
//                    Log.e(logTag, "Launching OAuth Process with no session view for $stateID")
//                    val url = ServerURLImpl.fromAddress(stateID.serverUrl, skipVerify)
//                    val intent = loginVM.newOAuthIntent(url, nextAction)
//                    intent?.let {
//                        withContext(Dispatchers.Main) {
//                            ContextCompat.startActivity(context, intent, null)
//                        }
//                    }
//                }
//            }
//
//            else -> {
//                Log.e(logTag, "Unexpected state: $stateID, route: ${startingState?.route}")
//                Thread.dumpStack()
//            }
//        }
    }

    private fun afterAuth(stateID: StateID, nextAction: String?) {
        Log.i(logTag, "... After OAuth: $stateID, next action: $nextAction")
        val route = BrowseDestinations.Open.createRoute(stateID)
        ackStartStateProcessed(null, stateID)
        navigateTo(route)
        loginVM.flush()
    }
}
