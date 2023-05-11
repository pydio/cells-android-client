package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.models.deduplicateNodes
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Expose methods used by bookmark pages */
class BookmarksVM(
    private val accountID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val transferService: TransferService,
) : AbstractBrowseVM(prefs, nodeService) {

    private val logTag = "BookmarksVM"

    private val orderFlow = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookmarks: StateFlow<List<MultipleItem>> = orderFlow.flatMapLatest { currPair ->
        nodeService.listBookmarkFlow(accountID, currPair.first, currPair.second).map { nodes ->
            deduplicateNodes(nodeService, nodes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = listOf()
    )

    fun forceRefresh(stateID: StateID) {
        viewModelScope.launch {
            launchProcessing()
            try {
                nodeService.refreshBookmarks(stateID)
                done()
            } catch (e: Exception) {
                done(e)
//                val msg = if (e is SDKException && e.message != null) e.message!! else {
//                    "Unexpected error while refreshing bookmarks for $stateID"
//                }
//                Log.e(logTag, msg)
//                done(e)
            }
        }
    }

    fun removeBookmark(stateID: StateID) {
        viewModelScope.launch {
            try {
                nodeService.toggleBookmark(stateID, false)
            } catch (e: Exception) {
                Log.e(
                    logTag,
                    "Unhandled error while removing bookmark for $stateID, cause:  ${e.message}"
                )
                e.printStackTrace()
            }
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

//    // Represents different states for the Bookmark screen
//    sealed class BookmarksUiState {
//        data class Success(val news: List<BookmarkItem>) : BookmarksUiState()
//        data class Error(val exception: Throwable) : BookmarksUiState()
//        object Loading : BookmarksUiState()
//    }
}
