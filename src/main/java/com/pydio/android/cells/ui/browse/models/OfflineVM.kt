package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Expose methods used by Offline pages */
class OfflineVM(
    stateID: StateID,
    prefs: PreferencesService,
    private val nodeService: NodeService,
    private val networkService: NetworkService,
    private val jobService: JobService,
    private val transferService: TransferService,
    private val offlineService: OfflineService,
) : AbstractBrowseVM(prefs, nodeService) {


    private val logTag = "OfflineVM"

    private val accountID = stateID.account()

    //    private val _loadingState = MutableLiveData(LoadingState.IDLE)
//    private val _errorMessage = MutableLiveData<String?>()
    private val _syncJobID = MutableLiveData(-1L)

//    val loadingState: LiveData<LoadingState> = _loadingState
//    val errorMessage: LiveData<String?> = _errorMessage

    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = sortOrder.switchMap { currOrder ->
            nodeService.listOfflineRoots(accountID, currOrder)
        }

    val syncJob: LiveData<RJob?>
        get() = _syncJobID.switchMap { currID ->
            if (currID < 1) {
                jobService.getMostRecent(offlineService.getSyncTemplateId(accountID))
            } else {
                jobService.getLiveJob(currID)
            }
        }

    fun download(stateID: StateID, uri: Uri) {
        viewModelScope.launch {
            transferService.saveToSharedStorage(stateID, uri)
        }
    }

    fun removeFromOffline(stateID: StateID) {
        viewModelScope.launch {
            offlineService.toggleOffline(stateID, false)
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
            offlineService.syncOfflineRoot(stateID)
        }
    }

    private suspend fun doForceAccountSync(accID: StateID) {

        val (jobID, error) = offlineService.prepareAccountSync(accID, AppNames.JOB_OWNER_USER)

        if (Str.notEmpty(error)) {
            _errorMessage.value = error
            return
        }

        _syncJobID.value = jobID
        jobService.launched(jobID)
        CellsApp.instance.appScope.launch {
            offlineService.performAccountSync(
                accID,
                jobID,
                CellsApp.instance.applicationContext
            )
        }
    }
}
