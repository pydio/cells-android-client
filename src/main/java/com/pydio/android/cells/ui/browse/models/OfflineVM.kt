package com.pydio.android.cells.ui.browse.models

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Expose methods used by Offline pages */
class OfflineVM(
    stateID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val networkService: NetworkService,
    private val jobService: JobService,
) : ViewModel() {

    private val logTag = "OfflineVM"

    private val accountID = stateID.account()

    private val listPrefs = prefs.cellsPreferencesFlow.map { cellsPreferences ->
        cellsPreferences.list
    }

    private val sortOrder = listPrefs.map { it.order }.asLiveData(viewModelScope.coroutineContext)
    val layout = listPrefs.map { it.layout }

//    private var livePrefs: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
//    private val sortOrder = livePrefs.getString(
//        AppKeys.CURR_RECYCLER_ORDER,
//        AppNames.DEFAULT_SORT_BY
//    )
    // val layout = livePrefs.getLayout(AppKeys.CURR_RECYCLER_LAYOUT, ListLayout.LIST)

    private val _loadingState = MutableLiveData(LoadingState.IDLE)
    private val _errorMessage = MutableLiveData<String?>()
    private val _syncJobID = MutableLiveData(-1L)

    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = Transformations.switchMap(sortOrder) { currOrder ->
            nodeService.listOfflineRoots(accountID, currOrder)
        }

    val syncJob: LiveData<RJob?>
        get() = Transformations.switchMap(
            _syncJobID
        ) { currID ->
            if (currID < 1) {
                MutableLiveData()
            } else {
                Log.e(logTag, "Updating sync job ID: #$currID")
                jobService.getLiveJob(currID)
            }
        }

    /** Exposed Business Methods **/
    fun setListLayout(listLayout: ListLayout) {
        viewModelScope.launch {
            prefs.setListLayout(listLayout)
        }
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

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            nodeService.saveToSharedStorage(stateID, uri)
        }
    }

    fun removeFromOffline(stateID: StateID) {
        viewModelScope.launch {
            nodeService.toggleOffline(stateID, false)
        }
    }

    fun forceFullSync() {
        val (ok, msg) = canLaunchSync(accountID)
        if (ok) {
            _errorMessage.value = null
        } else {
            _errorMessage.value = msg
            return
        }

        Log.e(logTag, "Setting loading state to PROCESSING")
        _loadingState.value = LoadingState.PROCESSING
        viewModelScope.launch {
            doForceAccountSync(accountID) // we insure the current account value is valid in the sanity check
            // TODO handle errors
            delay(1500)
            Log.e(logTag, "Setting loading state to IDLE")
            withContext(Dispatchers.Main) {
                _loadingState.value = LoadingState.IDLE
            }
        }
    }

    fun forceSync(stateID: StateID) {
        val (ok, msg) = canLaunchSync(stateID)
        if (ok) {
            _errorMessage.value = null
        } else {
            _errorMessage.value = msg
            return
        }

        Log.e(logTag, "Setting loading state to PROCESSING")
        _loadingState.value = LoadingState.PROCESSING
        viewModelScope.launch {
            doForceSingleRootSync(stateID)
            Log.e(logTag, "Setting loading state to IDLE")
            withContext(Dispatchers.Main) {
                _loadingState.value = LoadingState.IDLE
            }
        }
    }

    private fun canLaunchSync(stateID: StateID?): Pair<Boolean, String?> {
        return when (networkService.networkStatus) {
            is NetworkStatus.Metered -> {
                // TODO implement settings to force accept this user story
                Pair(false, "Preventing re-sync on metered network")
            }
            is NetworkStatus.Roaming -> {
                // TODO implement settings to force accept this user story
                Pair(false, "Preventing re-sync when on roaming network")
            }
            is NetworkStatus.Unavailable, is NetworkStatus.Unknown -> {
                Pair(false, "Cannot launch re-sync with no internet connection")
            }
            is NetworkStatus.Unmetered -> {
                return stateID?.let {
                    if (it != StateID.NONE) {
                        Pair(true, null)
                    } else {
                        Pair(false, "Cannot launch re-sync without choosing a target")
                    }
                } ?: Pair(false, "Cannot launch re-sync without choosing a target")
            }
        }
    }

    private fun doForceSingleRootSync(stateID: StateID) {
        CellsApp.instance.appScope.launch {
            nodeService.syncOfflineRoot(stateID)
        }
    }

    private suspend fun doForceAccountSync(accID: StateID) {

        val (jobID, error) = nodeService.prepareAccountSync(accID, AppNames.JOB_OWNER_USER)

        if (Str.notEmpty(error)) {
            _errorMessage.value = error
            return
        }

        _syncJobID.value = jobID
        jobService.launched(jobID)
        CellsApp.instance.appScope.launch {
            nodeService.performAccountSync(
                accID,
                jobID,
                CellsApp.instance.applicationContext
            )
        }
    }
}
