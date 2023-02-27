package com.pydio.android.cells.ui.browse.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.ui.core.LoadingState
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logTag = OfflineVM::class.simpleName

/** Expose methods used by Offline pages */
class OfflineVM(
    //  private val accountService: AccountService,
    private val nodeService: NodeService,
    private val networkService: NetworkService,
    private val jobService: JobService,
) : ViewModel() {

    private val _loadingState = MutableLiveData(LoadingState.STARTING)
    private val _errorMessage = MutableLiveData<String?>()
    private val _accountID: MutableLiveData<StateID> = MutableLiveData(Transport.UNDEFINED_STATE_ID)
    private val _syncJobID = MutableLiveData(-1L)

    val loadingState: LiveData<LoadingState> = _loadingState
    val errorMessage: LiveData<String?> = _errorMessage

    //    val loadingState: LiveData<LoadingState>
//        get() = _loadingState
//    val errorMessage: LiveData<String?>
//        get() = _errorMessage
    val offlineRoots: LiveData<List<RLiveOfflineRoot>>
        get() = Transformations.switchMap(
            _accountID
        ) { currID ->
            if (currID == Transport.UNDEFINED_STATE_ID) {
                MutableLiveData()
            } else {
                nodeService.listOfflineRoots(currID)
            }
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

    fun afterCreate(accountID: StateID) {
        _accountID.value = accountID
    }

    fun forceRefresh() {
        _errorMessage.value = null
        Log.e(logTag, "Setting loading state to idle")
        _loadingState.value = LoadingState.PROCESSING
        viewModelScope.launch {
            doForceRefresh()
            // TODO handle errors
            Log.e(logTag, "Setting loading state to idle")
            withContext(Dispatchers.Main){
                _loadingState.value = LoadingState.IDLE
            }
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
                _accountID.value?.let { accID ->
                    if (accID != Transport.UNDEFINED_STATE_ID) {
                        val (jobID, error) = nodeService.prepareAccountSync(
                            accID,
                            AppNames.JOB_OWNER_USER
                        )

                        if (Str.notEmpty(error)) {
                            _errorMessage.value = error
                        } else {
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
                }
            }
        }
    }
}
