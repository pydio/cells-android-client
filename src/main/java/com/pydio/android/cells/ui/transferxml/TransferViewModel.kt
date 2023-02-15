package com.pydio.android.cells.ui.transferxml

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import com.pydio.android.cells.AppKeys
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
    prefs: CellsPreferences,
    val transferService: TransferService,
    state: String?
) : ViewModel() {

    private val logTag = TransferViewModel::class.simpleName
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    private val stateID = StateID.fromId(state)

    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())

    private val liveFilterStatus: MutableLiveData<String> = liveSharedPreferences.getString(
        AppKeys.JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )
    fun getTransfersWithLiveFilter(): LiveData<List<RTransfer>> {
        return Transformations.switchMap(
            liveFilterStatus
        ) { name ->
            Log.e(logTag, "filter has changed: $name")
            transferService.queryTransfers(stateID)
        }
    }

    // Manage UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private var _oldFilter = prefs.getString(
        AppKeys.JOB_FILTER_BY_STATUS,
        AppNames.JOB_STATUS_NO_FILTER
    )
    val oldFilter: String
        get() = _oldFilter

    private var oldSortBy = prefs.getString(
        AppKeys.TRANSFER_SORT_BY,
        AppNames.JOB_SORT_BY_DEFAULT
    )

    override fun onCleared() {
        super.onCleared()
        vmJob.cancel()
    }

    private fun reQuery(value: String) {
        liveFilterStatus.value = value
    }

    init {
        // reQuery(_oldFilter)

        vmScope.launch {
            liveSharedPreferences.getString(
                AppKeys.TRANSFER_FILTER_BY_STATUS,
                AppNames.JOB_STATUS_NO_FILTER
            ).asFlow().collect {
                it?.let {
                    if (it != _oldFilter) {
                        reQuery(it)
                        _oldFilter = it
                    }
                }
            }
            liveSharedPreferences.getString(
                AppKeys.TRANSFER_SORT_BY,
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
