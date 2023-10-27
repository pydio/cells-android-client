package com.pydio.android.cells.ui.system

import androidx.navigation.NavHostController
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.cells.transport.StateID

// private val logTag = "SystemNavigation"

/** Defines the System and Settings destinations **/
sealed class SystemDestinations(val route: String) {

    companion object {
        protected const val PREFIX = "share"
    }

    data object About : SystemDestinations("about")
    data object Settings : SystemDestinations("settings")
    data object Logs : SystemDestinations("logs")
    data object Jobs : SystemDestinations("jobs")

    data object ClearCache : SystemDestinations("$PREFIX/clear-cache/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/clear-cache/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/clear-cache/") ?: false
    }
}

class SystemNavigationActions(navController: NavHostController) {

    val navigateToAbout: () -> Unit = {
        navController.navigate(SystemDestinations.About.route) {
            launchSingleTop = true
        }
    }

    val navigateToSettings: () -> Unit = {
        navController.navigate(SystemDestinations.Settings.route) {
            launchSingleTop = true
        }
    }

    val navigateToLogs: () -> Unit = {
        navController.navigate(SystemDestinations.Logs.route) {
            launchSingleTop = true
        }
    }

    val navigateToJobs: () -> Unit = {
        navController.navigate(SystemDestinations.Jobs.route) {
            launchSingleTop = true
        }
    }

    val navigateToClearCache: (StateID) -> Unit = { stateID ->
        navController.navigate(SystemDestinations.ClearCache.createRoute(stateID)) {
            launchSingleTop = true
        }
    }

    val back: () -> Unit = {
        navController.popBackStack()
    }
}
