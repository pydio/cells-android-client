package com.pydio.android.cells.ui.share

import androidx.navigation.NavHostController
import com.pydio.cells.transport.StateID

/** Simply expose navigation actions for the Share subGraph */
class ShareNavigation(private val navController: NavHostController) {

    // private val logTag = "ShareNavigation"

    fun toAccounts() {
        val route = ShareDestination.ChooseAccount.route
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun toFolder(stateID: StateID) {
        val route = ShareDestination.OpenFolder.createRoute(stateID)
        navController.navigate(route)
    }

    fun toTransfers(stateID: StateID, jobID: Long) {
        val route = ShareDestination.UploadInProgress.createRoute(stateID, jobID)
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun back() {
        navController.popBackStack()
    }
}
