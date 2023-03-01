package com.pydio.android.cells.ui.login

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.login.models.NewLoginVM
import com.pydio.android.cells.ui.login.screens.AskServerUrl
import com.pydio.android.cells.ui.login.screens.P8Credentials
import com.pydio.android.cells.ui.login.screens.ProcessAuth
import com.pydio.android.cells.ui.login.screens.SkipVerify
import com.pydio.android.cells.ui.login.screens.StartingLoginProcess

private const val logTag = "loginNavGraph"

fun NavGraphBuilder.loginNavGraph(
    helper: LoginHelper,
    loginVM: NewLoginVM,
//    back: () -> Unit,
//    navigateTo: (String) -> Unit,
    // open: (StateID) -> Unit,
//    isExpandedScreen: Boolean,
) {

    composable(LoginDestinations.Starting.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        Log.e(logTag, "... Starting login activity with $stateID")
        StartingLoginProcess()
    }

    composable(LoginDestinations.Done.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        Log.e(logTag, "... Starting done activity for $stateID")
        StartingLoginProcess()
    }

    composable(LoginDestinations.AskUrl.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        Log.e(logTag, "... Starting AskUrl activity for $stateID")
        AskServerUrl(helper = helper, loginVM = loginVM)
    }

//    composable(LoginDestinations.SkipVerify.route) { nbsEntry ->
//        val stateID = lazyStateID(nbsEntry)
//        Log.e(logTag, "... Starting SkipVerify activity for $stateID")
//        SkipVerify(helper = helper, loginVM = loginVM)
//    }
//
//    composable(LoginDestinations.P8Credentials.route) { nbsEntry ->
//        val stateID = lazyStateID(nbsEntry)
//        Log.e(logTag, "... Starting P8Credentials activity for $stateID")
//        P8Credentials(stateVM = null, loginVM = loginVM, navigateTo = navigateTo)
//    }
//
    composable(LoginDestinations.ProcessAuth.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        Log.e(logTag, "... Starting ProcessAuth activity for $stateID")
        ProcessAuth(
            stateID = stateID,
            loginVM = loginVM,
            helper = helper,
        )
    }
}
