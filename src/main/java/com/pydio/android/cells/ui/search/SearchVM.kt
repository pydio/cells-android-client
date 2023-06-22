package com.pydio.android.cells.ui.search

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.ui.models.MultipleItem
import com.pydio.android.cells.ui.models.deduplicateNodes
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
    private val transferService: TransferService,
) : AbstractCellsVM() {

    private val logTag = "SearchVM"

    private val _currQueryContext = MutableStateFlow("browse")
    private var _localStateID: StateID = stateID
    private val _userInput = MutableStateFlow("")

    // We debounce the user input before launching the potentially costly request to the server
    @OptIn(FlowPreview::class)
    private var _queryString: Flow<String> = _userInput.debounce(800L)

    // Used to directly update the search text field in the UI
    val userInput: StateFlow<String>
        get() = _userInput

    @OptIn(ExperimentalCoroutinesApi::class)
    val newHits: StateFlow<List<MultipleItem>> = defaultOrder
        .combine(_queryString) { order, query -> order to query }
        .flatMapLatest { currPair ->
            val (order, query) = currPair
            nodeService.liveSearch(
                _localStateID.account(),
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
                    if (loadingState.value != LoadingState.SERVER_UNREACHABLE) {
                        // skip remote process when the server is unreachable
                        nodeService.remoteQuery(_localStateID.account(), query)
                    }
                    Log.i(logTag, "Done with query: $query")
                    done()
                }
            }
        }
    }

    fun newContext(queryContext: String, stateID: StateID) {
        _currQueryContext.value = queryContext
        _localStateID = stateID
    }

    fun setQuery(query: String) {
        Log.d(logTag, "Setting query to: $query")
        _userInput.value = query
    }

    suspend fun retrieveFolder(stateID: StateID): Boolean {
        val (changeNb, errMsg) = nodeService.pull(stateID)
        // TODO improve error handling
        Log.e(logTag, "Retrieved folder at $stateID with $changeNb changes - Error: $errMsg")
        return Str.empty(errMsg)
    }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            try {
                transferService.saveToSharedStorage(stateID, uri)
            } catch (e: Exception) {
                done(e)
            }
        }
    }
}
