package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Expose methods used by bookmark pages */
class BookmarksVM(
    private val accountID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val transferService: TransferService,
) : AbstractBrowseVM(prefs, nodeService) {

    private val logTag = "BookmarksVM"

    private val orderPair = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }.asLiveData(viewModelScope.coroutineContext)
    val bookmarks: LiveData<List<RTreeNode>>
        get() = orderPair.switchMap { currOrder ->
            nodeService.listBookmarks(accountID, currOrder.first, currOrder.second)
        }

    fun forceRefresh(stateID: StateID) {
        viewModelScope.launch {
            launchProcessing()
            try {
                nodeService.refreshBookmarks(stateID)
                done()
            } catch (e: Exception) {
                val msg = if (e is SDKException && e.message != null) e.message!! else {
                    "Unexpected error while refreshing bookmarks for $stateID"
                }
                Log.e(logTag, msg)
                done(msg)
            }
        }
    }

    fun removeBookmark(stateID: StateID) {
        viewModelScope.launch {
            nodeService.toggleBookmark(stateID, false)
        }
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            transferService.saveToSharedStorage(stateID, uri)
        }
    }

    /* Helpers */
    init {
        forceRefresh(accountID)
        Log.d(logTag, "Initialising BookmarksVM")
    }

    override fun onCleared() {
        Log.d(logTag, "BookmarksVM cleared")
    }
}
