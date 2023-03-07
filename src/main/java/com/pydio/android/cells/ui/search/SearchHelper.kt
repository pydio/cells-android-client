package com.pydio.android.cells.ui.search

import android.content.Context
import android.util.Log
import androidx.navigation.NavHostController
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.cells.transport.StateID

class SearchHelper(
    private val navController: NavHostController,
    private val searchVM: SearchVM,
) {
    private val logTag = SearchHelper::class.simpleName

    suspend fun open(context: Context, stateID: StateID) {
        Log.d(logTag, "... Calling open for $stateID")
        searchVM.getNode(stateID)?.let {
            if (it.isFolder()) {
                navController.navigate(
                    BrowseDestinations.Open.createRoute(stateID)
                )
            } else {
                searchVM.viewFile(context, stateID)
            }
        }
    }

    fun cancel() {
        navController.popBackStack()
    }

    fun forceRefresh() {
        searchVM.doQuery()
    }
}