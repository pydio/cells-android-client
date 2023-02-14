package com.pydio.android.cells.ui.login

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.StartingState
import org.koin.androidx.compose.koinViewModel

private const val logTag = "LoginHost"

/** Main host for the navigation between login screens */
@Composable
fun LoginHost(
    startingState: StartingState,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    back: () -> Unit
) {

    // WARNING: this is a NavController **inside** another one
    val navController: NavHostController = rememberNavController()

    val loginVM: LoginViewModelNew = koinViewModel()
    val afterAuth: (Boolean) -> Unit = {
        val next = loginVM.nextAction.value
        Log.i(logTag, "After auth, success: $it, next action: $next")
        navController.popBackStack(RouteLoginUrl.route, true)
        // TODO handle next action
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

    LaunchedEffect(key1 = startingState) {
        if (RouteLoginProcessAuth.route == startingState.destination) {
            // We assume that the check on code and state auth has already been done at this point
            navController.navigate(RouteLoginProcessAuth.route)
            val res = loginVM.handleOAuthResponse(
                state = startingState.state!!,
                code = startingState.code!!,
            )
            if (res) {
                // FIXME
                navController.popBackStack(RouteLoginUrl.route, true)
                // TODO handle next action
                Log.e(logTag, "After nav, curr destination: ${navController.currentDestination}")
                if (navController.currentDestination == null) {
                    back()
                }
            }
        }
    }

//    val launchOAuth: (Intent) -> Unit = {
//        Log.d(logTag, "Launching OAuth flow with $it")
//        launchIntent(it, false, false)
//        // authActivity.finishAndRemoveTask()
//    }

    val navigateTo: (String?) -> Unit = { dest ->
        if (dest == null) {
            val res = navController.navigateUp()
            Log.e(logTag, "... Got a back request, could process: $res")
            if (!res) {
                back()
            }
        } else if (RouteLoginDone.route == dest) {
            afterAuth(true)
//            val res = navController.navigateUp()
//            Log.e(logTag, "... Got a back request, could process: $res")
//            if (!res) {
//                back()
//            }
//            launchIntent(null, false, false)
        } else {
            Log.e(logTag, "... Got a nav request for $dest")
            navController.navigate(dest)
            loginVM.switchLoading(false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = RouteLoginUrl.route,
    ) {
        RouteLoginUrl.composable(this, navController, loginVM, navigateTo)
        RouteLoginSkipVerify.composable(this, navController, loginVM, navigateTo)
        RouteLoginP8Credentials.composable(this, navController, loginVM, navigateTo)
        RouteLoginProcessAuth.composable(this, navController, loginVM, navigateTo)
    }
}
