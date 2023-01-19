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
import com.pydio.android.cells.SelectTargetDestination
import com.pydio.android.cells.ui.box.browse.FolderList
import com.pydio.android.cells.ui.box.browse.SessionList
import com.pydio.android.cells.ui.model.AccountListViewModel
import com.pydio.android.cells.ui.model.BrowseLocalFolders
import com.pydio.android.cells.ui.model.BrowseRemote
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "SelectTargetApp.kt"

@Composable
fun TargetSelectionHost(
    navController: NavHostController,
    action: String,
    initialStateId: String,
    browseLocalVM: BrowseLocalFolders,
    browseRemoteVM: BrowseRemote,
    accountListVM: AccountListViewModel,
    postActivity: (stateID: StateID) -> Unit,
    cancelActivity: () -> Unit,
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

    val login: (stateId: StateID) -> Unit = { stateId -> Log.e("TEST", "Login to $stateId") }

    var startDestination = if (initialStateId != Transport.UNDEFINED_STATE_ID) {
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
            SessionList(accountListVM, open, login)
        }
        composable(SelectTargetDestination.OpenFolder.route) { navBackStackEntry ->
            var stateId =
                navBackStackEntry.arguments?.getString(SelectTargetDestination.OpenFolder.getPathKey())
                    ?: initialStateId
            FolderList(
                action,
                stateId,
                currLoadingState ?: true,
                browseLocalVM,
                open,
                openParent,
                postActivity,
                cancelActivity
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
