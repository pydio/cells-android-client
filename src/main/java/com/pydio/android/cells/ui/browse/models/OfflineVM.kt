package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.preferences.defaultCellsPreferences
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.PreferencesService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractBrowseVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Expose methods used by Offline pages */
class OfflineVM(
    stateID: StateID,
    private val prefs: PreferencesService,
    private val nodeService: NodeService,
    private val networkService: NetworkService,
    private val jobService: JobService,
    private val transferService: TransferService,
    private val offlineService: OfflineService,
) : AbstractBrowseVM(prefs, nodeService) {

    private val logTag = "OfflineVM"

    private val accountID = stateID.account()

    private val _syncJobID = MutableStateFlow<Long>(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val offlineRoots: StateFlow<List<RLiveOfflineRoot>> =
        defaultListOrderFlow.flatMapLatest { currPair ->
            nodeService.listOfflineRoots(accountID, currPair.first, currPair.second)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val syncJob: StateFlow<RJob?> = _syncJobID.flatMapLatest { passedID ->
        // Not satisfying, if the job ID is not explicitly given, we retrieve the latest not done from the DB
        val currID = if (passedID < 1) {
            jobService.getLatestRunning(offlineService.getSyncTemplateId(accountID))?.jobId ?: -1
        } else {
            passedID
        }
        jobService.getLiveJobByID(currID)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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
        viewModelScope.launch {
            if (!checkBeforeLaunch(accountID)) {
                return@launch
            }
            doForceAccountSync(accountID) // we insure the current account value is valid in the sanity check
            delay(1500)
            Log.e(logTag, "Setting loading state to IDLE")
            done()
        }
    }

    fun forceSync(stateID: StateID) {
        viewModelScope.launch {
            if (!checkBeforeLaunch(stateID)) {
                return@launch
            }
            doForceSingleRootSync(stateID)
            Log.e(logTag, "Setting loading state to IDLE")
            done()
        }
    }

    private suspend fun checkBeforeLaunch(stateID: StateID): Boolean {
        val (ok, msg) = canLaunchSync(stateID.account())
        if (ok) {
            launchProcessing()
        } else {
            msg?.let { error(it) }
            return false
        }
        return true
    }

    private suspend fun canLaunchSync(stateID: StateID?): Pair<Boolean, String?> {

        val offlinePrefs = try {
            prefs.fetchPreferences()
        } catch (e: IllegalArgumentException) {
            defaultCellsPreferences()
        }

        return when (networkService.networkStatus) {
            is NetworkStatus.Unmetered -> {
                return stateID?.let {
                    if (it != StateID.NONE) {
                        Pair(true, null)
                    } else {
                        Pair(false, "Cannot launch re-sync without choosing a target")
                    }
                } ?: Pair(false, "Cannot launch re-sync without choosing a target")
            }

            is NetworkStatus.Metered -> {
                return if (offlinePrefs.meteredNetwork.applyLimits) {
                    Pair(false, "Preventing re-sync on metered network")
                } else {
                    return stateID?.let {
                        if (it != StateID.NONE) {
                            Pair(true, null)
                        } else {
                            Pair(false, "Cannot launch re-sync without choosing a target")
                        }
                    } ?: Pair(false, "Cannot launch re-sync without choosing a target")
                }
            }

            is NetworkStatus.Roaming -> {
                // TODO implement settings to force accept this user story
                Pair(false, "Preventing re-sync when on roaming network")
            }

            is NetworkStatus.Unavailable, is NetworkStatus.Unknown -> {
                Pair(false, "Cannot launch re-sync with no internet connection")
            }

        }
    }

    private fun doForceSingleRootSync(stateID: StateID) {
        viewModelScope.launch {
            offlineService.syncOfflineRoot(stateID)
        }
    }

    private suspend fun doForceAccountSync(accID: StateID) {

        val (jobID, error) = offlineService.prepareAccountSync(accID, AppNames.JOB_OWNER_USER)

        if (Str.notEmpty(error)) {
            error(error!!)
            return
        }

        _syncJobID.value = jobID
        jobService.launched(jobID)
        offlineService.performAccountSync(
            accID,
            jobID,
            CellsApp.instance.applicationContext
        )
    }

    init {
        done()
    }
}
