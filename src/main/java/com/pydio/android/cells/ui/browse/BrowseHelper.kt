package com.pydio.android.cells.ui.browse

import android.content.Context
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.ui.core.dumpNavigationStack
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID

open class BrowseHelper(
    private val navController: NavHostController,
    private val cellsVM: AbstractCellsVM,
) {
    private val logTag = "BrowseHelper"

    val browse = ListContext.BROWSE.id
    val bookmarks = ListContext.BOOKMARKS.id
    val offline = ListContext.OFFLINE.id

    suspend fun open(context: Context, stateID: StateID, callingContext: String = browse) {
        Log.i(
            logTag, "... Calling open for $stateID\n" +
                    "    Server reachable: ${cellsVM.isServerReachable()}\n" +
                    "    Context: $callingContext"
        )
        // TODO remove
        dumpNavigationStack(
            logTag, "BrowseHelper.open()",
            navController.currentBackStack.value, "OPEN $stateID"
        )

        val prevRoute = navController.previousBackStackEntry?.destination?.route
        val prevStateID = lazyStateID(navController.previousBackStackEntry, verbose = false)
        val currRoute = navController.currentBackStackEntry?.destination?.route
        val currStateID = lazyStateID(navController.currentBackStackEntry, verbose = false)

        // Kind of tweak: we check if the target node is the penultimate
        // element of the backStack, in such case we consider it is a "back":
        // the end user has clicked on parent() and was "simply" browsing
        val isEffectiveBack = BrowseDestinations.Open.isCurrent(prevRoute)
                && stateID == prevStateID
                && stateID == currStateID.parent()
        val isSame = BrowseDestinations.Open.isCurrent(currRoute)
                && stateID == currStateID

        if (isEffectiveBack) {
            Log.d(logTag, "Open node at $stateID is Effective Back")
            navController.popBackStack()
        } else if (isSame) {
            Log.w(logTag, "Target node $stateID is the *SAME* as the current route. Doing nothing")
        } else {
            val route: String
            if (!stateID.slug.isNullOrEmpty()) {
                val item = cellsVM.getNode(stateID) ?: run {
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
                    val isReachable = cellsVM.isServerReachable()
                    try {
                        cellsVM.viewFile(context, stateID, !isReachable)
                    } catch (e: SDKException) {
                        if (e.code == ErrorCodes.no_local_file || e.code == ErrorCodes.outdated_local_file) {
                            if (!isReachable) {
                                cellsVM.showError(
                                    ErrorMessage(
                                        "Cannot get un-cached file ${stateID.fileName}, server is unreachable",
                                        -1,
                                        listOf()
                                    )
                                )
                            } else {
                                val msg = if (e.code == ErrorCodes.outdated_local_file) {
                                    "Remote file has changed must be re-downloaded"
                                } else {
                                    "No local file, about to download"
                                }
                                Log.d(logTag, "... $msg")
                                // Open download popup
                                navController.navigate(
                                    CellsDestinations.Download.createRoute(stateID)
                                )
                            }
                            return
                        } else {
                            Log.e(logTag, "Unexpected error trying to open file: ${e.message}")
                            e.printStackTrace()
                            // throw e // FIXME: this makes the app crash, good for debugging but remove before going live
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

    fun navigate(route: String) {
        navController.navigate(route)
    }

    fun cancel() {
        navController.popBackStack()
    }
}
