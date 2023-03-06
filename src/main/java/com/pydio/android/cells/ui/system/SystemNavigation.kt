package com.pydio.android.cells.ui.system

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.pydio.cells.transport.StateID

// private val logTag = "SystemNavigation"

/** Defines the System and Settings destinations **/
sealed class SystemDestinations(val route: String) {

    companion object {
        protected const val STATE_ID_KEY = "state-id"
        protected const val PREFIX = "share"
    }

    object About : SystemDestinations("about")
    object Settings : SystemDestinations("settings")
    object Logs : SystemDestinations("logs")
    object Jobs : SystemDestinations("jobs")

    object ClearCache : SystemDestinations("$PREFIX/clear-cache/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/clear-cache/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/clear-cache/") ?: false
    }
}

class SystemNavigationActions(navController: NavHostController) {

    val navigateToAbout: () -> Unit = {
        navController.navigate(SystemDestinations.About.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToSettings: () -> Unit = {
        navController.navigate(SystemDestinations.Settings.route) {
            launchSingleTop = true
        }
    }

    val navigateToLogs: () -> Unit = {
        navController.navigate(SystemDestinations.Logs.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToJobs: () -> Unit = {
        navController.navigate(SystemDestinations.Jobs.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToClearCache: (StateID) -> Unit = { stateID ->
        val route = SystemDestinations.ClearCache.createRoute(stateID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    val back: () -> Unit = {
        navController.popBackStack()
    }
}
