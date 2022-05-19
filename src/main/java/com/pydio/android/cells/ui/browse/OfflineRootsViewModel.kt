package com.pydio.android.cells.ui.browse

import androidx.lifecycle.*
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.*
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.services.NodeService

/**
 * Holds a live list of the offline roots for the current session
 */
class OfflineRootsViewModel(
    private val nodeService: NodeService
) : ViewModel() {

    private val tag = OfflineRootsViewModel::class.simpleName
    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private lateinit var _stateId: StateID
    val stateId: StateID
        get() = _stateId

    private lateinit var _offlineRoots: LiveData<List<RLiveOfflineRoot>>
    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = _offlineRoots

    fun afterCreate(stateId: StateID){
        _stateId = stateId
        _offlineRoots = nodeService.listOfflineRoots(stateId)
    }

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun forceRefresh() {
        setLoading(true)
        vmScope.launch {
            withContext(Dispatchers.Main) {
                // TODO handle errors
                nodeService.runAccountSync(stateId)
                setLoading(false)
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

//    class OfflineRootsViewModelFactory(
//        private val nodeService: NodeService,
//        private val stateID: StateID,
//        private val application: Application
//    ) : ViewModelProvider.Factory {
//        @Suppress("unchecked_cast")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            if (modelClass.isAssignableFrom(OfflineRootsViewModel::class.java)) {
//                return OfflineRootsViewModel(nodeService, stateID, application) as T
//            }
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
}
