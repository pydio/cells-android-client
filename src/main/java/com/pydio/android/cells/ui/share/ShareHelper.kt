package com.pydio.android.cells.ui.share

import android.app.Activity
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.cells.transport.StateID

class ShareHelper(
    private val navController: NavHostController,
    private val processSelectedTarget: (StateID?) -> Unit,
    private val emitActivityResult: (Int) -> Unit,
//    private val startingState: StartingState?,
//    private val startingStateHasBeenProcessed: (String?, StateID) -> Unit,
) {
    private val logTag = "ShareHelper"
    private val navigation = ShareNavigation(navController)

    fun login(stateID: StateID, skipVerify: Boolean, isLegacy: Boolean) {
        val route = if (isLegacy) {
            Log.i(logTag, "... Launching re-log on P8 for $stateID")
            LoginDestinations.P8Credentials.createRoute(
                stateID,
                skipVerify,
                AuthService.LOGIN_CONTEXT_SHARE,
            )
        } else {
            Log.i(logTag, "... Launching re-log for ${stateID.account()} from $stateID")
            LoginDestinations.LaunchAuthProcessing.createRoute(
                stateID.account(),
                skipVerify,
                AuthService.LOGIN_CONTEXT_SHARE,
            )
        }
        navController.navigate(route)
    }

    /* Define callbacks */
    fun open(stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")
        if (stateID == StateID.NONE) {
            navigation.toAccounts()
        } else {
            navigation.toFolder(stateID)
        }

//        // TODO re-enable Tweak to keep the back stack lean
//        val bq = navController.backQueue
//        var isEffectiveBack = false
//        if (bq.size > 1) {
//            val penultimateID = lazyStateID(bq[bq.size - 2])
//            isEffectiveBack = penultimateID == stateID && StateID.NONE != stateID
//        }
//        if (isEffectiveBack) {
//            Log.d(logTag, "isEffectiveBack: $stateID")
//            navigation.back()
//        } else {
//            if (stateID == StateID.NONE) {
//                navigation.toAccounts()
//            } else {
//                navigation.toFolder(stateID)
//            }
//        }
    }

    fun cancel() {
        emitActivityResult(Activity.RESULT_CANCELED)
    }

    fun done() {
        emitActivityResult(Activity.RESULT_OK)
    }

    fun runInBackground() {
        emitActivityResult(Activity.RESULT_OK)
    }

    fun openParentLocation(stateID: StateID) {
        navigation.toParentLocation(stateID)
    }

    fun startUpload(stateID: StateID) { // shareVM: ShareVM,

        processSelectedTarget(stateID)

//        emitActivityResult
//
//        startingState?.let {
//            shareVM.launchPost(
//                stateID,
//                it.uris
//            ) { jobID -> afterLaunchUpload(stateID, jobID) }
//        } ?: run {
//            Log.e(logTag, "... No defined URIs, cannot post at $stateID")
//        }
    }

    // FIXME
//    private val afterLaunchUpload: (StateID, Long) -> Unit = { stateID, jobID ->
//        // Prevent double upload of the same file
//        startingStateHasBeenProcessed(null, stateID)
//        // then display the upload list
//        navigation.toTransfers(stateID, jobID)
//    }

    fun canPost(stateID: StateID): Boolean {
        // TODO also check permissions on remote server
        return !stateID.slug.isNullOrEmpty()
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
}
