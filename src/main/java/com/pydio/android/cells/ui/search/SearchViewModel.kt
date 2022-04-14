package com.pydio.android.cells.ui.search


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService

/**
 * Holds data when performing searches on files
 */
class SearchViewModel(private val nodeService: NodeService) : ViewModel() {

    //    private val tag = "SearchViewModel"
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private lateinit var _currentFolder: LiveData<RTreeNode>
    val currentFolder: LiveData<RTreeNode>
        get() = _currentFolder

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

    fun query(stateId: StateID, query: String) = vmScope.launch {
        setLoading(true)
        _queryString = query
        _hits.value = nodeService.remoteQuery(stateId, query)
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
//
//    class SearchViewModelFactory(
//        private val nodeService: NodeService,
//        private val stateID: StateID,
//        private val application: Application
//    ) : ViewModelProvider.Factory {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
//                return SearchViewModel(nodeService, stateID, application) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
}
