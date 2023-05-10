package com.pydio.android.cells.ui.search

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.models.ErrorMessage
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.models.deduplicateNodes
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds data when performing searches on files on a given remote server defined by its accountID
 *
 * Note that we pass the state ID as parameter (rather than the account ID) to enable future
 * improvements with finer searches, e.g. in this folder only
 * */
class SearchVM(
    stateID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val transferService: TransferService,
) : ViewModel() {

    private val logTag = "SearchVM"

    // TODO finalize this
    private val _currQueryContextF = MutableStateFlow<String>("browse")
    private val _loadingStateF = MutableStateFlow(LoadingState.NEW)
    private val _errorMessageF = MutableStateFlow<ErrorMessage?>(null)

    private val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }
    private val sortOrderFlow = listPrefs.map { it.order }

    private var localStateID: StateID = stateID

    private val _userInput = MutableStateFlow("")

    private val _currQueryContext = MutableLiveData("browse")
    private val _loadingState = MutableLiveData(LoadingState.NEW)
    private val _errorMessage = MutableLiveData<String?>()

    // Exposed to the UI
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage
    val layout = listPrefs.map { it.layout }

    // Used to directly update the search text field in the UI
    val userInput: StateFlow<String>
        get() = _userInput

    // We debounce the user input before launching the potentially costly request to the server
    @OptIn(FlowPreview::class)
    private var _queryString: Flow<String> = _userInput.debounce(800L)

    val hits = sortOrderFlow.combine(_queryString) { order, query ->
        nodeService.liveSearchFlow(
            localStateID.account(),
            if (Str.notEmpty(query)) query else "3c2babe5-2aa1-4fca-88ad-6b316c7cafe4", // TODO improve this to avoid querying the full repo when the sting is empty
            order
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val newHits: StateFlow<List<MultipleItem>> = sortOrderFlow
        .combine(_queryString) { order, query -> order to query }
        .flatMapLatest { currPair ->
            val (order, query) = currPair
            nodeService.liveSearchFlow(
                localStateID.account(),
                if (Str.notEmpty(query)) query else "3c2babe5-2aa1-4fca-88ad-6b316c7cafe4", // TODO improve this to avoid querying the full repo when the sting is empty
                order
            ).map { nodes ->
                deduplicateNodes(nodeService, nodes)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf()
        )

    init {
        viewModelScope.launch {
            _queryString.collect { query ->
                if (Str.notEmpty(query)) {
                    launchProcessing()
                    // TODO skip remote process when the server is unreachable
                    nodeService.remoteQuery(localStateID.account(), query)
                    Log.i(logTag, "Done with query: $query")
                    done()
                }
            }
        }
    }

    fun newContext(queryContext: String, stateID: StateID) {
        _currQueryContext.value = queryContext
        localStateID = stateID
    }

    fun setQuery(query: String) {
        Log.e(logTag, "Setting query to: $query")
        _userInput.value = query
    }

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
    private fun launchProcessing() {
        _loadingState.value = LoadingState.PROCESSING
        _errorMessage.value = null
    }

    private fun done(err: String? = null) {
        _loadingState.value = LoadingState.IDLE
        _errorMessage.value = err
    }
}
