package com.pydio.android.cells.ui.box

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ui.box.browse.SelectFolderScreen
import com.pydio.android.cells.ui.box.browse.SessionList
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseLocalFoldersVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "SelectTargetHost.kt"

sealed class SelectTargetDestination(val route: String) {

    object ChooseAccount : SelectTargetDestination("choose-account")

    object OpenFolder : SelectTargetDestination("open/{stateId}") {
        fun createRoute(stateId: StateID) = "open/${stateId.id}"
        fun getPathKey() = "stateId"
    }

    // TODO add a route that display the newly launched uploads with a "run in background option"
    // TODO add safety checks to prevent forbidden copy-move
    // --> to finalise we must really pass the node*s* to copy or move rather than its parent
}

@Composable
fun SelectTargetHost(
    navController: NavHostController,
    action: String,
    initialStateId: String,
    browseLocalVM: BrowseLocalFoldersVM,
    browseRemoteVM: BrowseRemoteVM,
    accountListVM: AccountListVM,
    postActivity: (stateID: StateID, action: String?) -> Unit,
) {

    val currLoadingState by browseRemoteVM.isLoading.observeAsState()

    /* Define callbacks */
    val open: (stateID: StateID) -> Unit = { stateID ->
        Log.d(logTag, "in open($stateID)")

        browseRemoteVM.watch(stateID)
        browseLocalVM.setState(stateID)
        // selectTargetVM.setCurrentState(stateID)
        val newRoute = SelectTargetDestination.OpenFolder.createRoute(stateID)
        Log.i(logTag, "About to navigate to [$newRoute]")
        navController.navigate(newRoute)
    }

    val openParent: (stateId: StateID) -> Unit = { stateId ->
        Log.d(logTag, ".... In OpenParent: $stateId - ${stateId.workspace} ")
        if (Str.empty(stateId.workspace)) {
            Log.d(logTag, "WS Root")
            navController.navigate(SelectTargetDestination.ChooseAccount.route)
        } else {
            Log.d(logTag, "##### Not a root")
            val parent = stateId.parent()
            Log.d(logTag, "##### Navigating to $parent")
            navController.navigate(SelectTargetDestination.OpenFolder.createRoute(parent))
        }
    }

    val canPost: (stateId: StateID) -> Boolean = { stateID ->
        true
//        if (action == AppNames.ACTION_UPLOAD) {
//            true
//        } else {
//            // Optimistic check to prevent trying to copy move inside itself
//            // TODO this does not work: we get the parent state as initial input
//            //   (to start from the correct location), we should rather get a list of states
//            //   that are about to copy / move to provide better behaviour in the select target activity
//            !((stateID.id.startsWith(initialStateId) && (stateID.id.length > initialStateId.length)))
//        }
    }

    val forceRefresh: (stateId: StateID) -> Unit = { browseRemoteVM.watch(it) }

    val startDestination = if (initialStateId != Transport.UNDEFINED_STATE_ID) {
        SelectTargetDestination.OpenFolder.route
    } else {
        SelectTargetDestination.ChooseAccount.route
    }

    /* Configure navigation */
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(SelectTargetDestination.ChooseAccount.route) {
            val login: (StateID) -> Unit = { postActivity(it, AppNames.ACTION_LOGIN) }
            SessionList(accountListVM, open, login)
        }
        composable(SelectTargetDestination.OpenFolder.route) { navBackStackEntry ->
            val stateId =
                navBackStackEntry.arguments?.getString(SelectTargetDestination.OpenFolder.getPathKey())
                    ?: initialStateId
            SelectFolderScreen(
                action,
                stateId,
                currLoadingState ?: true,
                browseLocalVM,
                open,
                openParent,
                canPost,
                postActivity,
                forceRefresh,
            )
        }
    }
}

@Composable
fun SelectTargetApp(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
