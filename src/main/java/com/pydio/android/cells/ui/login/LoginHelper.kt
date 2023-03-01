package com.pydio.android.cells.ui.login

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.browse.BrowseDestinations
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

    fun back() {
        navController.popBackStack()
    }

    fun afterPing(res: String) {
        navigateTo(res)
    }

    private suspend fun afterOAuth(stateID: StateID) {
        Log.e(logTag, "#########################")
        Log.e(logTag, "After Oauth: $stateID")
        val route = BrowseDestinations.Open.createRoute(stateID)
        startingStateHasBeenProcessed(null, stateID)
        loginVM.flush()
        navigateTo(route)
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
                        val stateID = StateID.fromId(it)
                        afterOAuth(stateID)
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
                }
//                    loginVM.toCellsCredentials(sessionView, "browse")
//                    // TODO also pop until home to prevent coming back on the server Url screen
                //    when the user clicks back
            }
        }
    }

}
