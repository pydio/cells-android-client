package com.pydio.android.cells.ui.system

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private val logTag = "SystemNavigation"

/** Defines the System and Settings destinations **/
object SystemDestinations {
    const val ABOUT_ROUTE = "about"
    const val SETTINGS_ROUTE = "settings"
    const val LOGS_ROUTE = "logs"
    const val JOBS_ROUTE = "jobs"
    const val CLEAR_CACHE_ROUTE = "clear-cache"
}


class SystemNavigationActions(navController: NavHostController) {

    val navigateToAbout: () -> Unit = {
        navController.navigate(SystemDestinations.ABOUT_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToSettings: () -> Unit = {
        navController.navigate(SystemDestinations.SETTINGS_ROUTE) {
            launchSingleTop = true
        }
    }


    val navigateToLogs: () -> Unit = {
        navController.navigate(SystemDestinations.LOGS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToJobs: () -> Unit = {
        navController.navigate(SystemDestinations.JOBS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateToClearCache: () -> Unit = {
        navController.navigate(SystemDestinations.CLEAR_CACHE_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val back: () -> Unit = {
        navController.popBackStack()
    }
}
