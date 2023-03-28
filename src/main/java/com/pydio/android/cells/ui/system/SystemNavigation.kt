package com.pydio.android.cells.ui.system

import androidx.navigation.NavHostController
import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.StateID

// private val logTag = "SystemNavigation"

/** Defines the System and Settings destinations **/
sealed class SystemDestinations(val route: String) {

    companion object {
        protected const val PREFIX = "share"
    }

    object About : SystemDestinations("about")
    object Settings : SystemDestinations("settings")
    object Logs : SystemDestinations("logs")
    object Jobs : SystemDestinations("jobs")

    object ClearCache : SystemDestinations("$PREFIX/clear-cache/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/clear-cache/${stateID.id}"
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
