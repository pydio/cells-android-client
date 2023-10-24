package com.pydio.android.cells.ui.search

import android.content.Context
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.BrowseHelper
import com.pydio.cells.transport.StateID

class SearchHelper(
    private val navController: NavHostController,
    private val searchVM: SearchVM,
) : BrowseHelper(navController, searchVM) {

    private val logTag = "SearchHelper"

    suspend fun openParentLocation(stateID: StateID) {
        val parent = stateID.parent()
        searchVM.getNode(parent)?.let {
            navController.navigate(
                BrowseDestinations.Open.createRoute(parent)
            )
        } ?: run {
            if (searchVM.retrieveFolder(parent)) {
                navController.navigate(BrowseDestinations.Open.createRoute(parent))
            }
        }
    }

    suspend fun open(context: Context, stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")
        searchVM.getNode(stateID)?.let {
            if (it.isFolder()) {
                navController.navigate(
                    BrowseDestinations.Open.createRoute(stateID)
                )
            } else {
                super.open(
                    context = context,
                    stateID = stateID,
                    callingContext = ListContext.SEARCH.id
                )
//                 searchVM.viewFile(context, stateID)
            }
        }
    }

}
