package com.pydio.android.cells.ui.search

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds data when performing searches on files on a given remote server defined by its accountID */
class SearchVM(
    private val stateID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = "SearchVM"

    private val _loadingState = MutableLiveData(LoadingState.NEW)
    private val _errorMessage = MutableLiveData<String?>()
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

    private val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }
    private val sortOrderFlow = listPrefs.map { it.order }
    val layout = listPrefs.map { it.layout }


    private val orderPair = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        prefs.getOrderByPair(
            cellsPreferences,
            ListType.DEFAULT
        )
    }.asLiveData(viewModelScope.coroutineContext)

//}: LiveData<List<RTreeNode>>
//        get() = orderPair.switchMap { currOrder ->
//            nodeService.listBookmarks(accountID, currOrder.first, currOrder.second)
//        }


    // TODO double check this we pass the StateID
    //  Both by injection and explicitly with the context
    private val _currID = MutableLiveData(stateID)
    private val _currQueryContext = MutableLiveData("browse")

    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String>
        get() = _userInput

    @OptIn(FlowPreview::class)
    private var _queryString: Flow<String> = _userInput.debounce(800L)

    private val _hits = MutableStateFlow<List<RTreeNode>>(listOf())
    val hits: StateFlow<List<RTreeNode>>
        get() = _hits


    val newHits = sortOrderFlow.combine(_queryString) { order, query ->
        nodeService.liveSearch(
            stateID.account(),
            if (Str.notEmpty(query)) query else "3c2babe5-2aa1-4fca-88ad-6b316c7cafe4", // TODO improve this to avoid querying the full repo when the sting is empty
            order
        )
    }

    init {
        viewModelScope.launch {
            _queryString.combine(sortOrderFlow) { q, o -> q to o }.collect { curr ->
                val (query, order) = curr
                if (Str.notEmpty(query)) {
                    launchProcessing()
                    // TODO skip remote process when the server is unreachable
                    nodeService.remoteQuery(stateID.account(), query)

                    Log.e(logTag, "Done with query: $query")
                    _hits.emit(
                        nodeService.searchLocally(
                            _currID.value?.account() ?: StateID.NONE,
                            query,
                            order
                        )
                    )
                    done()
                }
            }
        }
    }

    fun newContext(queryContext: String, stateID: StateID) {
        _currQueryContext.value = queryContext
        _currID.value = stateID
    }

//    val newHits: LiveData<List<RTreeNode>>
//        get() = queryString.switchMap { query ->
//            // For the time being, we always search in **all workspaces**, also in the remote server
//            vmScope.launch {
//                if (Str.notEmpty(query)) {
//                    nodeService.remoteQuery(stateID.account(), query)
//                    Log.e(logTag, "Done with query: $query")
//                }
//            }
//            nodeService.liveLocalQuery(stateID.account(), query)
//        }

    fun setQuery(query: String) {
        Log.e(logTag, "Setting query to: $query")
        // _queryString.value = query
        _userInput.value = query
    }

//    fun query(query: String) {
//        setQuery(query)
//        doQuery()
//    }
//
//    fun doQuery() = vmScope.launch {
//        if (Str.empty(queryString.value)) {
//            _errorMessage.value = "Please enter a non empty search query"
//            return@launch
//        }
//        launchProcessing()
//        done() // We must turn-off loading before setting the value for the error message to be correct
//        //      _hits.value = results
//    }

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
    }

    suspend fun retrieveFolder(stateID: StateID): Boolean {
        val (changeNb, errMsg) = nodeService.pull(stateID)
        // TODO improve error handling
        Log.e(logTag, "Retrieved folder at $stateID with $changeNb changes - Error: $errMsg")
        return Str.empty(errMsg)
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

    fun setListLayout(listLayout: ListLayout) {
        viewModelScope.launch {
            prefs.setListLayout(listLayout)
        }
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            transferService.saveToSharedStorage(stateID, uri)
        }
    }


    // Helpers

//    override fun onCleared() {
//        super.onCleared()
//    //     viewModelJob.cancel()
//    }

    private fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    private fun done(err: String? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = err
    }

}
