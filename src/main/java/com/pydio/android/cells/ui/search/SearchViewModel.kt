package com.pydio.android.cells.ui.search


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Holds data when performing searches on files on a given remote server defined by its accountID */
class SearchViewModel(
    encodedAccountId: String,
    private val nodeService: NodeService

) : ViewModel() {

    // private val logTag = SearchViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val accountId = StateID.fromId(encodedAccountId)

    private val _hits = MutableLiveData<List<RTreeNode>>()
    val hits: LiveData<List<RTreeNode>>
        get() = _hits

    private var _queryString: String? = ""
    val queryString: String?
        get() = _queryString

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun setQuery(query: String){
        _queryString = query
    }

    fun query(query: String) {
        setQuery(query)
        doQuery()
    }

    fun doQuery() = vmScope.launch {
        if (Str.empty(queryString)){
            _errorMessage.value = "Please enter a non empty search query"
            return@launch
        }
        setLoading(true)
        _hits.value = nodeService.remoteQuery(accountId, queryString!!)
//         _hits.value = nodeService.queryLocally(query, stateID)
        setLoading(false)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

}
