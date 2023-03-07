package com.pydio.android.cells.ui.search


import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Holds data when performing searches on files on a given remote server defined by its accountID */
class SearchVM(
    private val stateID: StateID,
    private val prefs: CellsPreferences,
    private val nodeService: NodeService
) : ViewModel() {

    private val logTag = SearchViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _loadingState = MutableLiveData(LoadingState.STARTING)
    private val _errorMessage = MutableLiveData<String?>()
    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

    private var livePrefs = LiveSharedPreferences(prefs.get())
    val layout = livePrefs.getLayout(AppKeys.CURR_RECYCLER_LAYOUT, ListLayout.LIST)

    private val _currQueryContext = MutableLiveData("browse")
    private val _currID = MutableLiveData(StateID.NONE)
    private var _queryString: MutableLiveData<String> = MutableLiveData("")
    val queryString: LiveData<String> = _queryString

    private val _hits = MutableLiveData<List<RTreeNode>>()
    val hits: LiveData<List<RTreeNode>>
        get() = _hits

    fun newContext(queryContext: String, stateID: StateID) {
        _currQueryContext.value = queryContext
        _currID.value = stateID
    }

    val newHits: LiveData<List<RTreeNode>>
        get() = Transformations.switchMap(
            queryString
        ) { query ->
            vmScope.launch {
                nodeService.remoteQuery(stateID, query)
                Log.e(logTag, "Done with query: $query")
            }
            nodeService.liveLocalQuery(stateID, query)
        }

    fun setQuery(query: String) {
        Log.e(logTag, "Setting query to: $query")
        _queryString.value = query
    }

    fun query(query: String) {
        setQuery(query)
        doQuery()
    }

    fun doQuery() = vmScope.launch {
        if (Str.empty(queryString.value)) {
            _errorMessage.value = "Please enter a non empty search query"
            return@launch
        }
        launchProcessing()
        done() // We must turn-off loading before setting the value for the error message to be correct
        //      _hits.value = results
    }

    suspend fun getNode(stateID: StateID): RTreeNode? {
        return nodeService.getNode(stateID)
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
        prefs.setString(AppKeys.CURR_RECYCLER_LAYOUT, listLayout.name)
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            nodeService.saveToSharedStorage(stateID, uri)
        }
    }


    // Helpers

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
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
