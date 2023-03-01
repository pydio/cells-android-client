package com.pydio.android.cells.ui.models

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hold a list of recent file transfers for current session.
 */
class UploadsVM(
    prefs: CellsPreferences,
    val transferService: TransferService,
) : ViewModel() {

    private val logTag = UploadsVM::class.simpleName
    private var vmJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + vmJob)

    // TODO rather inject this
    private val cr = CellsApp.instance.contentResolver

    private lateinit var _accountID: StateID

    private val currIds: MutableLiveData<Set<Long>> = MutableLiveData<Set<Long>>(mutableSetOf());

    private lateinit var _currRecords: LiveData<List<RTransfer>>
    val currRecords: LiveData<List<RTransfer>>
        get() = _currRecords

    // private val currUploads: MutableMap<Long, UploadState> = HashMap()

    fun launchShareToPydioAt(stateID: StateID, uris: List<Uri>) {
        setAccountID(stateID.account())
        val ids: MutableMap<Long, Pair<String, Uri>> = HashMap()

        vmScope.launch {
            // First only register the uploads
            for (uri in uris) {
                Log.e(logTag, "#### processing $uri ")
                try {
                    val tid = transferService.register(cr, uri, stateID)
                    ids[tid.first] = Pair(tid.second, uri)
                    setIds(ids.keys)
                } catch (e: Exception) {
                    // TODO handle this
                }
            }
            // Launch the 2 steps process
            ids.forEach {
                val (currName, currUri) = it.value
                transferService.launchCopy(cr, currUri, stateID, it.key, currName)?.let {
                    launch {
                        try {
                            transferService.uploadOne(it)
                            Log.w(logTag, "... $it ==> upload DONE")
                        } catch (e: Exception) {
                            Log.e(logTag, "... $it ==> upload FAILED: ${e.message}")
                        }
                    }
                    Log.w(logTag, "... $it ==> upload LAUNCHED")
                } ?: run {
                    // TODO better error management
                    Log.e(logTag, "could not upload $currName at $stateID")
                }
            }
        }
    }

    suspend fun get(transferId: Long): RTransfer? = withContext(Dispatchers.IO) {
        transferService.getRecord(_accountID, transferId)
    }

    fun pauseOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(_accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun resumeOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.uploadOne(_accountID, transferId)
        }
    }

    fun cancelOne(transferId: Long) {
        vmScope.launch {
            // TODO improve this
            transferService.cancelTransfer(_accountID, transferId, AppNames.JOB_OWNER_USER)
        }
    }

    fun removeOne(transferId: Long) {
        vmScope.launch {
            transferService.deleteRecord(_accountID, transferId)
        }
    }

    fun cancelAll() {
        currIds.value?.forEach {
            try {
                cancelOne(it)
            } catch (e: Exception) {
                Log.e(logTag, "could not cancel job #$it, cause: ${e.message}")
            }
        }
    }

    private fun setAccountID(accountID: StateID) {
        _accountID = accountID
        _currRecords = Transformations.switchMap(
            currIds
        ) { ids -> transferService.getCurrentTransfersRecords(_accountID, ids) }
    }

    private fun setIds(ids: Set<Long>) {
        currIds.value = ids
    }

//    // TODO re-implement support for filter and sort
//    private var liveSharedPreferences: LiveSharedPreferences = LiveSharedPreferences(prefs.get())
//
//    private val liveFilterStatus: MutableLiveData<String> = liveSharedPreferences.getString(
//        AppKeys.JOB_FILTER_BY_STATUS,
//        AppNames.JOB_STATUS_NO_FILTER
//    )
//
//    fun getCurrentTransfers(stateID: StateID): LiveData<List<RTransfer>> {
//        return transferService.queryTransfers(stateID)
//    }

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


//    // TODO rather retrieve on the properties than on the full object
//    open inner class ItemState (
//        private val rTransfer: RTransfer,
//    )  {
//
//        fun isFailed(): Boolean {
//            return Str.notEmpty(rTransfer.error)
//        }
//
//        fun isDone(): Boolean {
//            return rTransfer.doneTimestamp > 0
//        }
//
//        val ids: MutableMap<Long, Pair<String, Uri>> = HashMap()
//
//    }

    inner class UploadState(
        val transferId: Long,
        val uri: Uri,
        val fname: String,
    ) {
        private var _status: String = AppNames.JOB_STATUS_NEW
        val status: String
            get() = _status

        fun updateStatus(newStatus: String) {
            _status = newStatus
        }
    }


//    private fun reQuery(value: String) {
//        liveFilterStatus.value = value
//    }
//
//    init {
//        // reQuery(_oldFilter)
//
//        vmScope.launch {
//            liveSharedPreferences.getString(
//                AppKeys.TRANSFER_FILTER_BY_STATUS,
//                AppNames.JOB_STATUS_NO_FILTER
//            ).asFlow().collect {
//                // it?.let {
//                if (it != _oldFilter) {
//                    _oldFilter = it
//                    reQuery(it)
//                } else { // this should never happen
//                    Log.w(logTag, "Received a new event for same query, this should not happen")
//                }
//                // }
//            }
//            liveSharedPreferences.getString(
//                AppKeys.TRANSFER_SORT_BY,
//                AppNames.JOB_SORT_BY_DEFAULT
//            ).asFlow().collect {
//                //it?.let {
//                if (it != oldSortBy) {
//                    oldSortBy = it
//                    reQuery(it)
//                }
//                // }
//            }
//        }
//    }
}