package com.pydio.android.cells.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Holds a live list of the offline roots for the current session
 */
class OfflineRootsViewModel(
    private val nodeService: NodeService,
    private val jobService: JobService
) : ViewModel() {

    // private val tag = OfflineRootsViewModel::class.simpleName

    private var viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    lateinit var runningSync: LiveData<RJob?>

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
        runningSync = nodeService.getRunningAccountSync(stateId.account())
    }

    fun forceRefresh() {
        _errorMessage.value = null
        setLoading(true)
        vmScope.launch {
            // TODO insure a sync is not already running for this account.
            val (jobId, error) = nodeService.prepareAccountSync(stateId, AppNames.JOB_OWNER_USER)
            if (Str.notEmpty(error)) {
                _errorMessage.value = error
            } else {
                jobService.launched(jobId)
                CellsApp.instance.appScope.launch {
                    nodeService.performAccountSync(stateId, jobId)
                }
            }
            setLoading(false)
        }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
