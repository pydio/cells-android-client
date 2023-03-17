package com.pydio.android.cells.ui.browse.models

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Expose methods used by bookmark pages */
class BookmarksVM(
    private val accountID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService
) : ViewModel() {

    private val logTag = "BookmarksVM"

    private val _loadingState = MutableLiveData(LoadingState.STARTING)
    private val _errorMessage = MutableLiveData<String?>()
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

//     val cellsPreferences = prefs.cellsPreferencesFlow

    val layout = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list.layout
    }
//    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
//    private val _layout = MutableStateFlow(ListLayout.LIST)
//    val layout: StateFlow<ListLayout> = _layout.asStateFlow()

    init {
        Log.d(logTag, "Initialising BookmarksVM")

//        viewModelScope.launch {
//            liveSharedPreferences.getString(
//                AppKeys.CURR_RECYCLER_LAYOUT,
//                AppNames.RECYCLER_LAYOUT_LIST
//            ).asFlow().collect {
//                if (it != _layout.value.name) {
//                    val newValue = try {
//                        ListLayout.valueOf(it)
//                    } catch (e: IllegalArgumentException) {
//                        ListLayout.LIST
//                    }
//                    _layout.value = newValue
//                }
//            }
//        }

//        viewModelScope.launch {
//            val currUserPrefs = prefs.fetchInitialPreferences()
//            Log.e(logTag, "~~~ Got preferences, version: ${currUserPrefs.versionCode}")
//        }
    }

    private val orderPair = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }.asLiveData(viewModelScope.coroutineContext)
    val bookmarks: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(
            orderPair
        ) { currOrder ->
            nodeService.listBookmarks(accountID, currOrder.first, currOrder.second)
        }

//    fun afterCreate(accountID: StateID) {
//        _accountID.value = accountID
//    }

    fun removeBookmark(stateID: StateID) {
        viewModelScope.launch {
            nodeService.toggleBookmark(stateID, false)
        }
    }

    fun forceRefresh(stateID: StateID) {
        viewModelScope.launch {
            launchProcessing()
            done(nodeService.refreshBookmarks(stateID))
        }
    }

    fun setListLayout(listLayout: ListLayout) {
//         prefs.setString(AppKeys.CURR_RECYCLER_LAYOUT, listLayout.name)
        viewModelScope.launch {
            prefs.setListLayout(listLayout)
        }
    }

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            nodeService.saveToSharedStorage(stateID, uri)
        }
    }

    suspend fun viewFile(context: Context, stateID: StateID) {
        getNode(stateID)?.let { node ->
            // TODO was nodeService.getLocalFile(it, activeSessionVM.canDownloadFiles())
            //    re-implement finer check of the current context (typically metered state)
            //    user choices.
            nodeService.getLocalFile(node, true)?.let { file ->
                externallyView(context, file, node)
            }
        }
    }
    /* Helpers */

    private fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    private fun done(err: String? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = err
    }
}
