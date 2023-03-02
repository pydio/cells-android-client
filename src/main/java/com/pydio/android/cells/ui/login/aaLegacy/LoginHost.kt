package com.pydio.android.cells.ui.login

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.login.aaLegacy.LoginViewModelNew
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private const val logTag = "LoginHost"

/** Main host for the navigation between login screens */
@Composable
fun LoginHost(
    currAccount: StateID,
    openAccount: (StateID) -> Unit,
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    back: () -> Unit,
    navController: NavHostController = rememberNavController()
) {

    val loginVM: LoginViewModelNew = koinViewModel()

    val afterAuth: (Boolean) -> Unit = {
        val next = loginVM.nextAction.value
        Log.i(logTag, "After auth, success: $it, next action: $next")
        // navController.popBackStack(RouteLoginStarting.route, true)
        // TODO handle next action

        Log.e(logTag, "Printing the back stack:")
        var i = 0
        navController.backQueue.forEach { nbse ->
            Log.e(logTag, "#${i.inc()}: ${nbse}")

        }
        Log.e(logTag, "After nav, curr destination: ${navController.currentDestination}")
        if (navController.currentDestination == null) {
            back()
        }
    }

    LaunchedEffect(true) {
        loginVM.oauthIntent.collect {
            if (it != null) {
                launchIntent(it, false, false)
            }
        }
    }

    LaunchedEffect(key1 = currAccount, key2 = startingState) {
        startingState?.let { state ->
//            if (RouteLoginProcessAuth.route == state.destination) {
//                // OAuth flow Callback
//                // We assume that the check on code and state auth has already been done at this point
//                navController.navigate(RouteLoginProcessAuth.route)
//                val res = loginVM.handleOAuthResponse(
//                    state = state.state!!,
//                    code = state.code!!,
//                )
//                if (res) {
//                    loginVM.accountId.value?.let {
//                        val stateID = StateID.fromId(it)
//                        openAccount(stateID)
//                        startingStateHasBeenProcessed(null, stateID)
//                    } ?: run {
//                        startingStateHasBeenProcessed(null, Transport.UNDEFINED_STATE_ID)
//                    }
//                    // FIXME
//                    navController.popBackStack(RouteLoginUrl.route, true)
//                    // TODO handle next action
//                    Log.e(
//                        logTag,
//                        "After nav, curr destination: ${navController.currentDestination}"
//                    )
//                    if (navController.currentDestination == null) {
//                        back()
//                    }
//                }
//            }
        } ?: run {
//            if (currAccount != Transport.UNDEFINED_STATE_ID) {
//
//                // We are in the case of a relog
//                loginVM.getSessionView(currAccount)?.let { sessionView ->
//                    // FIXME implement next
//                    loginVM.toCellsCredentials(sessionView, "browse")
//
//                    // TODO also pop until home to prevent coming back on the server Url screen
//                    //    when the user clicks back
//                    if (sessionView.isLegacy) {
//                        navController.navigate(RouteLoginP8Credentials.route) {}
//                    } else {
//                        navController.navigate(RouteLoginProcessAuth.route) {}
//                    }
//                }
//            } else {
//                // register a new account
//                navController.navigate(RouteLoginUrl.route) {}
//            }
        }
    }

    val navigateTo: (String?) -> Unit = { dest ->
        if (dest == null) {
            val res = navController.navigateUp()
            Log.e(logTag, "... Got a back request, could process: $res")
            if (!res) {
                back()
            }
//        } else if (RouteLoginDone.route == dest) {
//            afterAuth(true)
        } else {
            Log.e(logTag, "... Got a nav request for $dest")
            navController.navigate(dest)
            loginVM.switchLoading(false)
        }
    }

//    NavHost(
//        navController = navController,
//        startDestination = RouteLoginStarting.route,
//    ) {
//        RouteLoginStarting.composable(this, navController, loginVM, navigateTo)
//        RouteLoginUrl.composable(this, navController, loginVM, navigateTo)
//        RouteLoginSkipVerify.composable(this, navController, loginVM, navigateTo)
//        RouteLoginP8Credentials.composable(this, navController, loginVM, navigateTo)
//        RouteLoginProcessAuth.composable(this, navController, loginVM, navigateTo)
//    }
}
