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
import com.pydio.android.cells.ui.models.BookmarkItem
import com.pydio.cells.api.SDKException
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
    val bookmarks: StateFlow<List<BookmarkItem>> = orderFlow.flatMapLatest { currPair ->
        nodeService.listBookmarkFlow(accountID, currPair.first, currPair.second).map { nodes ->
            var bis: MutableList<BookmarkItem> = mutableListOf()
            // Also manage a short cache for the referenced workspace
            val wss: MutableMap<String, String> = mutableMapOf()

            for (node in nodes) {
                // We cannot rely on the fact that nodes are ordered: distinct bookmarked nodes
                // with same size or name that have "more than one path" (e.g are also in a cell)
                // might get mixed up.
                val newItem = BookmarkItem(
                    uuid = node.uuid,
                    mime = node.mime,
                    eTag = node.etag,
                    name = node.name,
                    sortName = node.sortName ?: node.name,
                    size = node.size,
                    remoteModTs = node.remoteModificationTS,
                    hasThumb = node.hasThumb(),
                    isFolder = node.isFolder(),
                )
                val slug = node.getStateID().slug!!
                if (!wss.containsKey(slug)) {
                    nodeService.getWorkspace(node.getStateID().workspace())?.let {
                        wss[slug] = it.label ?: slug
                    } ?: run {
                        wss[slug] = slug
                    }
                }
                // We manually insure that we only reference each effective target once => might be sub-optimal for very large systems
                val existingIndex = bis.indexOf(newItem)
                if (existingIndex > -1) {
                    val currItem = bis[existingIndex]
                    currItem.appearsIn.add(node.getStateID())
                    currItem.appearsInWorkspace[slug] = wss[slug] ?: slug
                } else {
                    newItem.appearsIn.add(node.getStateID())
                    newItem.appearsInWorkspace[slug] = wss[slug] ?: slug
                    bis.add(newItem)
                }
            }
            bis
        }
    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5000),
        initialValue = listOf()
    )

    private val orderPair = orderFlow.asLiveData(viewModelScope.coroutineContext)

    val bookmarksOld: LiveData<List<RTreeNode>>
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
