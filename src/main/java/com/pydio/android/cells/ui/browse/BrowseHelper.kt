package com.pydio.android.cells.ui.browse

import android.content.Context
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.browse.models.FolderVM
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

class BrowseHelper(
    private val currFolderStateID: StateID,
    private val navController: NavHostController,
    private val browseRemoteVM: BrowseRemoteVM,
    private val folderVM: FolderVM,
) {
    private val logTag = "BrowseHelper"

    suspend fun open(context: Context, stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")

        // Kind of tweak: we check if the target node is the penultimate
        // element of the backStack, in such case we consider it is a back:
        // the end user has clicked on parent() and was "simply" browsing
        // Log.d(logTag, "### Opening state at $it, Backstack: ")
        val bq = navController.backQueue
        // var i = 0
        // navController.backQueue.forEach {
        //     val stateID = lazyStateID(it)
        //     Log.e(logTag, "#${i++} - $stateID - ${it.destination.route}")

        // }
        var isEffectiveBack = false
        if (bq.size > 1) {
            val targetEntry = bq[bq.size - 2]
            val penultimateID = lazyStateID(bq[bq.size - 2])
            isEffectiveBack =
                BrowseDestinations.Open.isCurrent(targetEntry.destination.route)
                        && penultimateID == stateID && stateID != StateID.NONE
        }
        if (isEffectiveBack) {
            Log.e(logTag, "Open node at $stateID is Effective Back")
            navController.popBackStack()
        } else {
            val route: String
            if (Str.notEmpty(stateID.workspace)) {
                val item = folderVM.getNode(stateID) ?: run {
                    // We cannot navigate to an unknown node item
                    Log.e(logTag, "No TreeNode found for $stateID in local repo, aborting")
                    return
                }
                route = if (item.isFolder()) {
                    BrowseDestinations.Open.createRoute(stateID)
                } else if (item.isPreViewable()) {
                    BrowseDestinations.OpenCarousel.createRoute(stateID)
                } else {
                    folderVM.viewFile(context, stateID)
                    return
                }
            } else if (stateID == StateID.NONE) {
                route = CellsDestinations.Accounts.route
            } else {
                route = BrowseDestinations.Open.createRoute(stateID)
            }
            navController.navigate(route)
        }
    }

    fun cancel() {
        navController.popBackStack()
    }

    fun forceRefresh() {
        browseRemoteVM.watch(currFolderStateID, true)
    }
}
