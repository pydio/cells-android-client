package com.pydio.android.cells.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun afterCreate(stateId: StateID) {
        _stateId = stateId
        _offlineRoots = nodeService.listOfflineRoots(stateId)
    }


    fun forceRefresh() {
        setLoading(true)
        vmScope.launch {
            withContext(Dispatchers.Main) {
                // TODO handle errors
                nodeService.runAccountSync(stateId, "user")
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
