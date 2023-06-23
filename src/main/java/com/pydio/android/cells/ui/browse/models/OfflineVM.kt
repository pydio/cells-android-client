package com.pydio.android.cells.ui.browse.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLiveOfflineRoot
import com.pydio.android.cells.db.preferences.defaultCellsPreferences
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NetworkService
import com.pydio.android.cells.services.NetworkStatus
import com.pydio.android.cells.services.OfflineService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.cells.transport.StateID
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
    private val networkService: NetworkService,
    private val jobService: JobService,
    private val transferService: TransferService,
    private val offlineService: OfflineService,
) : AbstractCellsVM() {

    private val logTag = "OfflineVM"

    private val accountID = stateID.account()

    private val _syncJobID = MutableStateFlow<Long>(-1L)

    // Observe the defined offline roots for current account
    @OptIn(ExperimentalCoroutinesApi::class)
    val offlineRoots: StateFlow<List<RLiveOfflineRoot>> =
        defaultOrderPair.flatMapLatest { currPair ->
            nodeService.listOfflineRoots(accountID, currPair.first, currPair.second)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf()
        )

    // Observe latest sync job
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
            try {
                transferService.saveToSharedStorage(stateID, uri)
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun removeFromOffline(stateID: StateID) {
        viewModelScope.launch {
            try {
                offlineService.toggleOffline(stateID, false)
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun forceFullSync() {
        viewModelScope.launch {
            try {
                if (!checkBeforeLaunch(accountID)) {
                    return@launch
                }
                doForceAccountSync(accountID) // we insure the current account value is valid in the sanity check
                delay(1500)
                Log.i(logTag, "Setting loading state to IDLE")
                done()
            } catch (e: Exception) {
                done(e)
            }
        }
    }

    fun forceSync(stateID: StateID) {
        viewModelScope.launch {
            if (!checkBeforeLaunch(stateID)) {
                return@launch
            }
            try {
                offlineService.syncOfflineRoot(stateID)
                done()
            } catch (e: Exception) {
                done(e)
            }
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

        return when (networkService.fetchNetworkStatus()) {
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

            is NetworkStatus.Unavailable, is NetworkStatus.Captive, is NetworkStatus.Unknown -> {
                Pair(false, "Cannot launch re-sync with no internet connection")
            }
        }
    }

    private suspend fun doForceAccountSync(accountID: StateID) {
        val jobID = offlineService.prepareAccountSync(accountID, AppNames.JOB_OWNER_USER)
        Log.e(logTag, "Account sync prepared, with job #$jobID")

        _syncJobID.value = jobID
        jobService.launched(jobID)
        offlineService.performAccountSync(
            accountID,
            jobID,
            CellsApp.instance.applicationContext
        )
    }

    init {
        done()
    }
}
