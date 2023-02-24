package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.navigation.NavController
import com.pydio.android.cells.AppNames
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

class ShareHelper(
    private val navController: NavController,
    private val launchTaskFor: (String, StateID) -> Unit,
) {

    private val logTag = ShareHelper::class.simpleName

    /* Define callbacks */
    fun open(stateID: StateID) {
        Log.d(logTag, "in open($stateID)")
        val newRoute = ShareDestination.OpenFolder.createRoute(stateID)
        Log.i(logTag, "About to navigate to [$newRoute]")
        navController.navigate(newRoute)
    }

    fun openParent(stateID: StateID) {
        Log.d(logTag, ".... In OpenParent: $stateID - ${stateID.workspace} ")
        if (Str.empty(stateID.workspace)) {
            navController.navigate(ShareDestination.ChooseAccount.route)
        } else {
            val parent = stateID.parent()
            navController.navigate(ShareDestination.OpenFolder.createRoute(parent))
        }
    }

    fun interceptPost(currAction: String, stateID: StateID) {

        if (AppNames.ACTION_UPLOAD == currAction) {
            navController.navigate(
                ShareDestination.UploadInProgress.createRoute(stateID),
            ) {
                // We insure that the navigation page is first on the back stack
                // So that the end user cannot launch the upload twice using the back btn
                popUpTo(ShareDestination.ChooseAccount.route) { inclusive = true }
            }
        }
        launchTaskFor(currAction, stateID)
    }

    fun canPost(stateID: StateID) {
        Str.notEmpty(stateID.workspace)
        // true
//        if (action == AppNames.ACTION_UPLOAD) {
//            true
//        } else {
//            // Optimistic check to prevent trying to copy move inside itself
//            // TODO this does not work: we get the parent state as initial input
//            //   (to start from the correct location), we should rather get a list of states
//            //   that are about to copy / move to provide better behaviour in the select target activity
//            !((stateID.id.startsWith(initialStateId) && (stateID.id.length > initialStateId.length)))
//        }
    }

    fun forceRefresh(it: StateID) {
        // FIXME
        // browseRemoteVM.watch(it, true)
    }

}