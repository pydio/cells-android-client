package com.pydio.android.cells.ui.browse

import android.content.Context
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.ui.browse.models.AbstractBrowseVM
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

class BrowseHelper(
    private val navController: NavHostController,
    private val browseVM: AbstractBrowseVM,
) {
    private val logTag = "BrowseHelper"

    val browse = ListContext.BROWSE.id
    val bookmarks = ListContext.BOOKMARKS.id
    val offline = ListContext.OFFLINE.id

    suspend fun open(context: Context, stateID: StateID, callingContext: String = browse) {
        Log.d(logTag, "... Calling open for $stateID")

        // Kind of tweak: we check if the target node is the penultimate
        // element of the backStack, in such case we consider it is a back:
        // the end user has clicked on parent() and was "simply" browsing
        // Log.d(logTag, "### Opening state at $it, Backstack: ")
//        val bq = navController.backQueue
        // var i = 0
        // navController.backQueue.forEach {
        //     val stateID = lazyStateID(it)
        //     Log.e(logTag, "#${i++} - $stateID - ${it.destination.route}")

        // }
        var isEffectiveBack = false
        if (navController.backQueue.size > 1) {
            val bq = navController.backQueue
            val targetEntry = bq[bq.size - 2]
            val penultimateID = lazyStateID(bq[bq.size - 2])
            isEffectiveBack =
                BrowseDestinations.Open.isCurrent(targetEntry.destination.route)
                        && penultimateID == stateID && stateID != StateID.NONE
        }
        if (isEffectiveBack) {
            Log.d(logTag, "Open node at $stateID is Effective Back")
            navController.popBackStack()
        } else {
            val route: String
            if (Str.notEmpty(stateID.workspace)) {
                val item = browseVM.getNode(stateID) ?: run {
                    // We cannot navigate to an unknown node item
                    Log.e(logTag, "No TreeNode found for $stateID in local repo, aborting")
                    return
                }
                route = if (item.isFolder()) {
                    BrowseDestinations.Open.createRoute(stateID)
                } else if (item.isPreViewable() && callingContext == browse) {
                    // TODO (since v2) Open carousel for bookmark, offline and search result nodes
                    BrowseDestinations.OpenCarousel.createRoute(stateID)
                } else {
                    try {
                        browseVM.viewFile(context, stateID)
                    } catch (e: SDKException) {
                        if (e.code == ErrorCodes.no_local_file) {
                            // File has not yet been downloaded
                            navController.navigate(CellsDestinations.Download.createRoute(stateID))
                            return
                        } else {
                            Log.e(
                                logTag,
                                "Unexpected error while trying to view file: ${e.message}"
                            )
                            e.printStackTrace()
                            throw e // FIXME: this makes the app crash, good for debugging but remove before going live
                        }
                    }
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

//    fun forceRefresh() {
//        browseRemoteVM.watch(currFolderStateID, true)
//    }
}
