package com.pydio.android.cells.ui.share

import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.share.models.ShareVM
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

class ShareHelper(
    private val navController: NavHostController,
    val launchTaskFor: (String, StateID) -> Unit,
    private val startingState: StartingState?,
    private val startingStateHasBeenProcessed: (String?, StateID) -> Unit,
) {
    private val logTag = ShareHelper::class.simpleName
    private val navigation = ShareNavigation(navController)

    /* Define callbacks */
    fun open(stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")
        // Tweak to keep the back stack lean
        val bq = navController.backQueue
        var isEffectiveBack = false
        if (bq.size > 1) {
            val penultimateID = lazyStateID(bq[bq.size - 2])
            isEffectiveBack = penultimateID == stateID && Transport.UNDEFINED_STATE_ID != stateID
        }
        if (isEffectiveBack) {
            Log.d(logTag, "isEffectiveBack: $stateID")
            navigation.back()
        } else {
            if (stateID == Transport.UNDEFINED_STATE_ID) {
                navigation.toAccounts()
            } else {
                navigation.toFolder(stateID)
            }
        }
    }

    fun startUpload(shareVM: ShareVM, stateID: StateID) {
        startingState?.let {
            shareVM.launchPost(
                stateID,
                it.uris
            ) { afterLaunchUpload(stateID, it) }

        } ?: run {
            Log.e(logTag, "... No defined URIs, cannot post at $stateID")
        }
    }

    // TODO do it more elegantly, this is the come back of the call back hell for the time being
    private val afterLaunchUpload: (StateID, Long) -> Unit = { stateID, jobID ->
        // TODO parameters are useless for now
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

//    fun forceRefresh(it: StateID) {
//        // FIXME
//        // browseRemoteVM.watch(it, true)
//    }
}
