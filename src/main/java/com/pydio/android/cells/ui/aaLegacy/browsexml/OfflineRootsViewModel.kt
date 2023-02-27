package com.pydio.android.cells.ui.aaLegacy.browsexml

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
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
    prefs: CellsPreferences,
    private val networkService: NetworkService,
    private val jobService: JobService,
    private val nodeService: NodeService
) : ViewModel() {

    // private val tag = OfflineRootsViewModel::class.simpleName

    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    private lateinit var _stateId: StateID
    val stateId: StateID
        get() = _stateId

    private lateinit var _offlineRoots: LiveData<List<RLiveOfflineRoot>>
    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = _offlineRoots

    lateinit var runningSync: LiveData<RJob?>

    // Cache list order to only trigger order change when necessary
    private var _currentOrder = prefs.getString(
        AppKeys.CURR_RECYCLER_ORDER,
        AppNames.DEFAULT_SORT_ENCODED
    )

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

    fun orderHasChanged(newOrder: String): Boolean {
        return _currentOrder != newOrder
    }

    fun reQuery(newOrder: String) {
        if (_currentOrder != newOrder) {
            _currentOrder = newOrder
            _offlineRoots = nodeService.listOfflineRoots(stateId)
        }
    }

    fun forceRefresh() {
        _errorMessage.value = null
        setLoading(true)
        vmScope.launch {
            doForceRefresh()
            setLoading(false)
        }
    }

    private suspend fun doForceRefresh() {
        when (networkService.networkStatus) {
            is NetworkStatus.Metered -> {
                // TODO implement settings to force accept this user story
                _errorMessage.value = "Preventing re-sync on metered network"
            }
            is NetworkStatus.Roaming -> {
                // TODO implement settings to force accept this user story
                _errorMessage.value = "Preventing re-sync when on roaming network"
            }
            is NetworkStatus.Unavailable, is NetworkStatus.Unknown -> {
                _errorMessage.value = "Cannot launch re-sync with no internet connection"
            }
            is NetworkStatus.Unmetered -> {
                val (jobId, error) = nodeService.prepareAccountSync(
                    stateId,
                    AppNames.JOB_OWNER_USER
                )
                if (Str.notEmpty(error)) {
                    _errorMessage.value = error
                } else {
                    jobService.launched(jobId)
                    CellsApp.instance.appScope.launch {
                        nodeService.performAccountSync(
                            stateId,
                            jobId,
                            CellsApp.instance.applicationContext
                        )
                    }
                }
            }
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}
