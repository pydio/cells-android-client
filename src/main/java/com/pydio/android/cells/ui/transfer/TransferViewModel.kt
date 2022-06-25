package com.pydio.android.cells.ui.transfer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.reactive.LiveSharedPreferences
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Hold a list of recent file transfers for current session.
 */
class TransferViewModel(
    state: String?,
    prefs: CellsPreferences,
    val transferService: TransferService
) : ViewModel() {

    private val logTag = TransferViewModel::class.simpleName
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    private val stateID = StateID.fromId(state)

    private val lastEvent = MutableLiveData<String>()
    val transfers: LiveData<List<RTransfer>> = Transformations.switchMap(lastEvent) {
        transferService.queryTransfers(stateID)
    }

    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())

    private var oldFilter = prefs.getString(
        AppNames.PREF_KEY_JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )
    private var oldSortBy = prefs.getString(
        AppNames.PREF_KEY_TRANSFER_SORT_BY,
        AppNames.JOB_SORT_BY_DEFAULT
    )

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

    private fun reQuery(value: String) {
        lastEvent.value = value
    }

    init {
        Log.d(logTag, "init launched")
        reQuery("init")

        vmScope.launch {
            liveSharedPreferences.getString(
                AppNames.PREF_KEY_TRANSFER_FILTER_BY_STATUS,
                AppNames.JOB_STATUS_NO_FILTER
            ).asFlow().collect {
                it?.let {
                    if (it != oldFilter) {
                        reQuery(it)
                        oldFilter = it
                    }
                }
            }
            liveSharedPreferences.getString(
                AppNames.PREF_KEY_TRANSFER_SORT_BY,
                AppNames.JOB_SORT_BY_DEFAULT
            ).asFlow().collect {
                it?.let {
                    if (it != oldSortBy) {
                        reQuery(it)
                        oldSortBy = it
                    }
                }
            }
        }
    }
}
