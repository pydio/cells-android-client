package com.pydio.android.cells.ui.browse

import androidx.navigation.NavHostController
import com.pydio.cells.transport.StateID

/** Simply expose navigation actions for the Browse subGraph */
class BrowseNavigationActions(private val navController: NavHostController) {

    private val logTag = "BrowseNavigationActions"

    fun toBrowse(stateID: StateID) {
        val route = BrowseDestinations.Open.createRoute(stateID)
        navController.navigate(route)
        // We don't want the single top flag when browsing otherwise the native back button does not work
//        {launchSingleTop = true}
    }

    fun toOfflineRoots(stateID: StateID) {
        val route = BrowseDestinations.OfflineRoots.createRoute(stateID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun toBookmarks(stateID: StateID) {
        val route = BrowseDestinations.Bookmarks.createRoute(stateID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun toTransfers(stateID: StateID) {
        val route = BrowseDestinations.Transfers.createRoute(stateID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }
}
