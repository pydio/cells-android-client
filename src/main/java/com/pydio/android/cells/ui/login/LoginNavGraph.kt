package com.pydio.android.cells.ui.login

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.core.lazySkipVerify
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.login.screens.AskServerUrl
import com.pydio.android.cells.ui.login.screens.LaunchAuthProcessing
import com.pydio.android.cells.ui.login.screens.P8Credentials
import com.pydio.android.cells.ui.login.screens.ProcessAuth
import com.pydio.android.cells.ui.login.screens.SkipVerify
import com.pydio.android.cells.ui.login.screens.StartingLoginProcess

fun NavGraphBuilder.loginNavGraph(
    helper: LoginHelper,
    loginVM: LoginVM,
) {
    val logTag = "loginNavGraph"

    composable(LoginDestinations.Starting.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## 1st compo login/starting/$stateID")
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

    composable(LoginDestinations.AskUrl.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## 1st compo login/ask-url/$stateID")
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
        LaunchedEffect(key1 = stateID, key2 = skipVerify) {
            Log.i(logTag, "## 1st compo login/p8-creds/$stateID/$skipVerify")
        }
        P8Credentials(
            stateID = stateID,
            skipVerify = skipVerify,
            helper = helper,
            loginVM = loginVM,
        )
    }

    composable(LoginDestinations.LaunchAuthProcessing.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val skipVerify = lazySkipVerify(nbsEntry)
        LaunchedEffect(key1 = stateID, key2 = skipVerify) {
            Log.i(logTag, "## 1st compo login/launch-auth/$stateID/$skipVerify")
        }
        LaunchAuthProcessing(
            stateID = stateID,
            skipVerify = skipVerify,
            loginVM = loginVM,
            helper = helper,
        )
    }

    composable(LoginDestinations.ProcessAuthCallback.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        LaunchedEffect(key1 = stateID) {
            Log.i(logTag, "## 1st compo login/process-auth/$stateID")
        }
        ProcessAuth(
            stateID = stateID,
            loginVM = loginVM,
            helper = helper,
        )
    }
}
