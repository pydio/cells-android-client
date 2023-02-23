package com.pydio.android.cells.ui.nav

import android.util.Log
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.cells.transport.StateID

/**
 * Main destinations used in Cells App
 */
sealed class CellsDestinations(val route: String) {

    object Root : CellsDestinations("root")
    object Home : CellsDestinations("home")
    object Accounts : CellsDestinations("accounts")
    object Search : CellsDestinations("search")
    object ShareWith : CellsDestinations("share")

    // Sub routes
    // object System : CellsDestinations("system")


    object Login : CellsDestinations("login/{accountId}") {
        val prefix = "login"
        fun createRoute(accountID: StateID) = "login/${accountID.id}"
        fun getPathKey() = "accountId"
    }
//
//    object Browse : CellsDestinations("browse/{accountId}") {
//        fun createRoute(accountID: StateID) = "browse/${accountID.id}"
//        fun getPathKey() = "accountId"
//    }
}

class CellsNavigationActions(private val navController: NavHostController) {

    private val logTag = CellsNavigationActions::class.simpleName

    val navigateToHome: () -> Unit = {
        navController.navigate(CellsDestinations.Home.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when selecting the same item again
            launchSingleTop = true
            // Restore state when selecting again a previously selected item
            restoreState = true
        }
    }

    val navigateToAccounts: () -> Unit = {
        navController.navigate(CellsDestinations.Accounts.route) {
            // Remove other screens (TODO: Really?)
            // and only enable one copy for this destination
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
    }

    fun navigateToBrowse(stateID: StateID) {
        val route = BrowseDestinations.Open.createRoute(stateID)
        navController.navigate(route) {
            // FIXME
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToLogin(accountID: StateID) {
        val route = CellsDestinations.Login.createRoute(accountID)
        Log.e(logTag, "Open Login graph for $accountID: $route")
        navController.navigate(route) {
        }
    }

//    val navigateToSystem: (StateID?) -> Unit = {
//        Log.e(logTag, "Open System graph for $it")
//        navController.navigate(CellsDestinations.System.route) {
//        }
//    }
}
