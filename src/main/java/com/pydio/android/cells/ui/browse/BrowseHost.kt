package com.pydio.android.cells.ui.browse

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.browse.screens.AccountHome
import com.pydio.android.cells.ui.browse.screens.Folder
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel

sealed class BrowseDestination(val route: String) {

    object AccountHome : BrowseDestination("account-home")

    object OpenFolder : BrowseDestination("open/{stateId}") {
        fun createRoute(stateID: StateID) = "open/${stateID.id}"
        fun getPathKey() = "stateId"
    }

    // TODO implement other destinations
    // Bookmarks, Offline, Dialogs
}

private const val logTag = "BrowseHost"

/** Main host for the navigation while browsing a given account */
@Composable
fun BrowseHost(
    accountID: StateID,
    openAccounts: () -> Unit,
    back: () -> Unit,
    openDrawer: () -> Unit,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
) {

    val ctx = LocalContext.current

    val navController = rememberNavController()
    val currLoadingState by browseRemoteVM.isLoading.observeAsState()

    val open: (StateID) -> Unit = { stateID ->
        navController.navigate(
            if (Str.notEmpty(stateID.workspace)) {
                BrowseDestination.OpenFolder.createRoute(stateID)
            } else {
                BrowseDestination.AccountHome.route
            }
        )
    }

    val openParent: (StateID) -> Unit = { stateID ->
        val parent = stateID.parent()
        open(parent)
    }

    NavHost(
        navController = navController,
        startDestination = BrowseDestination.AccountHome.route
    ) {

        composable(BrowseDestination.AccountHome.route) {
            Log.e(logTag, ".... Open account home for $accountID")

            AccountHome(
                accountID,
                openDrawer = openDrawer,
                openAccounts = openAccounts,
                openSearch = {},
                openWorkspace = open,
                browseRemoteVM = browseRemoteVM,
            )
        }

        composable(BrowseDestination.OpenFolder.route) { navBackStackEntry ->
            val stateId =
                navBackStackEntry.arguments?.getString(BrowseDestination.OpenFolder.getPathKey())

            if (stateId == null) {
                Log.e(logTag, "... trying to browse with no state ID for $accountID")
                LaunchedEffect(key1 = accountID) {
                    // This should never happen
                    // We fall back on account home
                    navController.popBackStack(BrowseDestination.AccountHome.route, false)
                }

            } else {
                Folder(
                    StateID.fromId(stateId),
                    openDrawer = openDrawer,
                    openParent = openParent,
                    open = open,
                    openSearch = {},
                    browseRemoteVM = browseRemoteVM,
                )
            }
        }
    }
}
