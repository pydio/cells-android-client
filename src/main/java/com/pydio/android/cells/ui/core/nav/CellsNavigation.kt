package com.pydio.android.cells.ui.core.nav

import androidx.navigation.NavHostController
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.cells.transport.StateID

/**
 * Main generic destinations used in Cells App
 */
sealed class CellsDestinations(val route: String) {

    data object Root : CellsDestinations("root")
    data object Home : CellsDestinations("home")
    data object Accounts : CellsDestinations("accounts")

    data object Search :
        CellsDestinations("search/{${AppKeys.QUERY_CONTEXT}}/{${AppKeys.STATE_ID}}") {

        fun createRoute(queryContext: String, stateID: StateID) =
            "search/${queryContext}/${encodeStateForRoute(stateID)}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("search/") ?: false
    }

    data object Download :
        CellsDestinations("download/{${AppKeys.STATE_ID}}") {

        fun createRoute(stateID: StateID) =
            "download/${encodeStateForRoute(stateID)}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("download/") ?: false
    }
}

class CellsNavigationActions(private val navController: NavHostController) {

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
}
