package com.pydio.android.cells.ui.nav

import android.util.Log
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.pydio.cells.transport.StateID

/**
 * Main destinations used in Cells App
 */
object CellsDestinations {

    const val ROOT_ROUTE = "root"
    const val HOME_ROUTE = "home"
    const val ACCOUNTS_ROUTE = "accounts"
    const val SEARCH_ROUTE = "search"
    const val SHARE_WITH_PYDIO_ROUTE = "share"

    // Sub routes
    const val LOGIN_ROUTE = "login"
    const val BROWSE_ROUTE = "browse"
    const val SYSTEM_ROUTE = "system"
}

class CellsNavigationActions(navController: NavHostController) {

    private val logTag = CellsNavigationActions::class.simpleName

    val navigateToHome: () -> Unit = {
        navController.navigate(CellsDestinations.HOME_ROUTE) {
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
        navController.navigate(CellsDestinations.ACCOUNTS_ROUTE) {
            // Remove other screens (TODO :Really?)
            // and only enable one copy for this destination
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
    }

    val navigateToBrowse: (StateID) -> Unit = {
        navController.navigate(CellsDestinations.BROWSE_ROUTE) {

            // FIXME
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToLogin: (StateID) -> Unit = {
        Log.e(logTag, "Open Login graph for $it")
        navController.navigate(CellsDestinations.LOGIN_ROUTE) {
//            // FIXME
//            popUpTo(navController.graph.findStartDestination().id) {
//                saveState = true
//            }
//            // Avoid multiple copies of the same destination when selecting the same item again
//            launchSingleTop = true
//            // Restore state when selecting again a previously selected item
//            restoreState = true
        }
    }

    val navigateToSystem: (StateID?) -> Unit = {
        Log.e(logTag, "Open System graph for $it")
        navController.navigate(CellsDestinations.SYSTEM_ROUTE) {}
    }
}
