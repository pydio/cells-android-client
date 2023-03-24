package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Expose methods used by bookmark pages */
class BookmarksVM(
    private val accountID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    transferService: TransferService
) : AbstractBrowseVM(prefs, nodeService, transferService) {

    private val logTag = "BookmarksVM"

    private val _loadingState = MutableLiveData(LoadingState.STARTING)
    private val _errorMessage = MutableLiveData<String?>()
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

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
            done(nodeService.refreshBookmarks(stateID))
        }
    }

    fun removeBookmark(stateID: StateID) {
        viewModelScope.launch {
            nodeService.toggleBookmark(stateID, false)
        }
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            nodeService.saveToSharedStorage(stateID, uri)
        }
    }

    /* Helpers */
    init {
        Log.d(logTag, "Initialising BookmarksVM")
    }

    override fun onCleared() {
        Log.d(logTag, "BookmarksVM cleared")
    }

    private fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    private fun done(err: String? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = err
    }
}
