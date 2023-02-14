package com.pydio.android.cells.ui.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Main destinations used in Cells App
 */
object SystemDestinations {
    const val ABOUT_ROUTE = "about"
}

private val logTag = "SystemNavGraph"

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


}
