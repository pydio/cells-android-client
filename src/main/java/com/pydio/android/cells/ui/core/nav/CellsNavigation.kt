package com.pydio.android.cells.ui.core.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.StateID

/**
 * Main generic destinations used in Cells App
 */
sealed class CellsDestinations(val route: String) {

    object Root : CellsDestinations("root")
    object Home : CellsDestinations("home")
    object Accounts : CellsDestinations("accounts")

    object Search :
        CellsDestinations("search/{${AppKeys.QUERY_CONTEXT}}/{${AppKeys.STATE_ID}}") {

        fun createRoute(queryContext: String, stateID: StateID) =
            "search/${queryContext}/${stateID.id}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("search/") ?: false
    }
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
            popUpTo(CellsDestinations.Accounts.route) {
//                saveState = true
            }
            launchSingleTop = true
        }
    }

    /**
     * @param queryContext from where we want to search
     * @param stateID current stateID
     */
    fun navigateToSearch(queryContext: String, stateID: StateID) {
        val route = CellsDestinations.Search.createRoute(queryContext, stateID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

//    fun navigateToLogin(accountID: StateID) {
//        val route = CellsDestinations.Login.createRoute(accountID)
//        Log.e(logTag, "Open Login graph for $accountID: $route")
//        navController.navigate(route) {
//        }
//    }
}
