package com.pydio.android.cells.ui.login

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.core.lazyLoginContext
import com.pydio.android.cells.ui.core.lazySkipVerify
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.login.screens.AskServerUrl
import com.pydio.android.cells.ui.login.screens.LaunchOAuthFlow
import com.pydio.android.cells.ui.login.screens.P8Credentials
import com.pydio.android.cells.ui.login.screens.SkipVerify
import com.pydio.android.cells.ui.login.screens.StartingLoginProcess

fun NavGraphBuilder.loginNavGraph(
    helper: LoginHelper,
    loginVM: LoginVM,
) {
    val logTag = "loginNavGraph"

    composable(LoginDestinations.Starting.route) {
        // val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = Unit) {
            Log.i(logTag, "## 1st compo login/starting")
        }
        StartingLoginProcess()
    }

    composable(LoginDestinations.Done.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## 1st compo login/done/$stateID")
        }
        StartingLoginProcess()
    }

    composable(LoginDestinations.AskUrl.route) {
        LaunchedEffect(key1 = Unit) {
            Log.i(logTag, "## 1st compo login/ask-url")
        }
        AskServerUrl(helper = helper, loginVM = loginVM)
    }

    composable(LoginDestinations.SkipVerify.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## 1st compo login/skip-verify/$stateID")
        }
        SkipVerify(stateID, helper = helper, loginVM = loginVM)
    }

    composable(LoginDestinations.P8Credentials.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val skipVerify = lazySkipVerify(nbsEntry)
        val lContext = lazyLoginContext(nbsEntry)
        LaunchedEffect(key1 = stateID, key2 = skipVerify) {
            Log.i(logTag, "## 1st compo login/p8-creds/$stateID/$skipVerify/$lContext")
        }
        P8Credentials(
            stateID = stateID,
            skipVerify = skipVerify,
            loginContext = lContext,
            helper = helper,
            loginVM = loginVM,
        )
    }

    composable(LoginDestinations.LaunchAuthProcessing.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val skipVerify = lazySkipVerify(nbsEntry)
        val lContext = lazyLoginContext(nbsEntry)
        LaunchedEffect(key1 = stateID, key2 = skipVerify) {
            Log.i(logTag, "## 1st compo login/launch-auth/$stateID/$skipVerify/$lContext")
        }
        LaunchOAuthFlow(
            stateID = stateID,
            skipVerify = skipVerify,
            loginContext = lContext,
            loginVM = loginVM,
            helper = helper,
        )
    }
}
