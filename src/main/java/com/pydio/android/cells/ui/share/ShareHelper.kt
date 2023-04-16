package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

class ShareHelper(
    private val navController: NavHostController,
    val launchTaskFor: (String, StateID) -> Unit,
    private val startingState: StartingState?,
    private val startingStateHasBeenProcessed: (String?, StateID) -> Unit,
) {
    private val logTag = "ShareHelper"
    private val navigation = ShareNavigation(navController)

    /* Define callbacks */
    fun open(stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")
        // Tweak to keep the back stack lean
        val bq = navController.backQueue
        var isEffectiveBack = false
        if (bq.size > 1) {
            val penultimateID = lazyStateID(bq[bq.size - 2])
            isEffectiveBack = penultimateID == stateID && StateID.NONE != stateID
        }
        if (isEffectiveBack) {
            Log.d(logTag, "isEffectiveBack: $stateID")
            navigation.back()
        } else {
            if (stateID == StateID.NONE) {
                navigation.toAccounts()
            } else {
                navigation.toFolder(stateID)
            }
        }
    }

    fun cancel(stateID: StateID) {
        launchTaskFor(AppNames.ACTION_CANCEL, stateID)
    }

    fun done(stateID: StateID) {
        launchTaskFor(AppNames.ACTION_DONE, stateID)
    }

    fun runInBackground(stateID: StateID) {
        launchTaskFor(AppNames.ACTION_DONE, stateID)
    }

    fun openParentLocation(stateID: StateID) {
        navigation.toParentLocation(stateID)
    }

    fun startUpload(shareVM: ShareVM, stateID: StateID) {
        startingState?.let {
            shareVM.launchPost(
                stateID,
                it.uris
            ) { jobID -> afterLaunchUpload(stateID, jobID) }
        } ?: run {
            Log.e(logTag, "... No defined URIs, cannot post at $stateID")
        }
    }

    private val afterLaunchUpload: (StateID, Long) -> Unit = { stateID, jobID ->
        // Prevent double upload of the same file
        startingStateHasBeenProcessed(null, stateID)
        // then display the upload list
        navigation.toTransfers(stateID, jobID)
    }

    fun canPost(stateID: StateID): Boolean {
        // TODO also check permissions on remote server
        return Str.notEmpty(stateID.workspace)
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
