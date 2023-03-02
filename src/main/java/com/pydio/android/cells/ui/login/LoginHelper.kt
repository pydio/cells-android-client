package com.pydio.android.cells.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.login.models.NewLoginVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginHelper(
    private val navController: NavHostController,
    private val loginVM: NewLoginVM,
    val navigateTo: (String) -> Unit,
    val launchTaskFor: (String, StateID) -> Unit,
    val startingState: StartingState?,
    val startingStateHasBeenProcessed: (String?, StateID) -> Unit,
) {
    private val logTag = LoginHelper::class.simpleName
    private val navigation = LoginNavigation(navController)

    fun cancel() {
        navController.popBackStack()
        loginVM.flush()
    }

    suspend fun back() {
        val bq = navController.backQueue
        var i = 0
        navController.backQueue.forEach {
            val stateID = lazyStateID(it)
            Log.e(logTag, "#${i++} - ${it.destination.route} - $stateID ")
        }
        var isFirstLoginPage = true
        if (bq.size > 1) {
            val penEntry = bq[bq.size - 2]
            val penultimateID = lazyStateID(penEntry)
            val penRoute = penEntry.destination.route
            if (LoginDestinations.isCurrent(penRoute)) {
                // penultimate route is still in the login subgraph
                isFirstLoginPage = false
            }
        }
        if (isFirstLoginPage) { // back is then a cancellation of the current login process
            cancel()
        } else {
            loginVM.resetMessages()
            navController.popBackStack()
        }
    }

    fun afterPing(res: String) {
        navigateTo(res)
    }

    suspend fun launchP8Auth(login: String, pwd: String, captcha: String?) {
        val stateID = loginVM.logToP8(login, pwd, captcha)
        if (stateID != null) {
            // Login has been successful,
            // We clean after ourselves and leave the login subgraph
            afterAuth(stateID)
        } // else do nothing: error message has already been displayed and we stay on the page
    }

    suspend fun processAuth(context: Context, stateID: StateID) {
        startingState?.let { state ->
            if (LoginDestinations.ProcessAuth.isCurrent(state.destination)) {
                Log.d(logTag, "Processing OAuth response for $stateID and ${state.state}")
                // OAuth flow Callback
                val res = loginVM.handleOAuthResponse(
                    // We assume nullity has already been checked
                    state = state.state!!,
                    code = state.code!!,
                )
                if (res) {
                    Log.i(logTag, "OAuth OK - ${loginVM.accountId.value}")
                    loginVM.accountId.value?.let {
                        val currID = StateID.fromId(it)
                        afterAuth(currID)
                    } ?: run {
                        // TODO better error handling
                        startingStateHasBeenProcessed(null, Transport.UNDEFINED_STATE_ID)
                    }
                }
            }
        } ?: run {
            if (stateID != Transport.UNDEFINED_STATE_ID) {

                // The user wants to login again in an expired already registered account
                loginVM.getSessionView(stateID)?.let { sessionView ->
                    // FIXME implement next
                    val url = ServerURLImpl.fromAddress(sessionView.url, sessionView.skipVerify())
                    val intent = loginVM.newOAuthIntent(url)
                    intent?.let {
                        withContext(Dispatchers.Main) {
                            ContextCompat.startActivity(context, intent, null)
                        }
                    }
                } ?: run {
                    Log.e(logTag, "Launching OAuth Process with no session view for $stateID")
                    // FIXME handle self-signed scenario
                    val url = ServerURLImpl.fromAddress(stateID.serverUrl, false)
                    val intent = loginVM.newOAuthIntent(url)
                    intent?.let {
                        withContext(Dispatchers.Main) {
                            ContextCompat.startActivity(context, intent, null)
                        }
                    }
                }
            }
        }
    }

    private fun afterAuth(stateID: StateID) {
        Log.e(logTag, "#########################")
        Log.e(logTag, "After Oauth: $stateID")
        val route = BrowseDestinations.Open.createRoute(stateID)
        startingStateHasBeenProcessed(null, stateID)
        loginVM.flush()
        navigateTo(route)
    }
}
