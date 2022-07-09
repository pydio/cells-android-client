package com.pydio.android.cells.ui.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * View model to manage a single file download to the device
 */
class DownloadViewModel(
    val encodedState: String,
    val forwardAfterDownload: Boolean,
    val transferService: TransferService,
    val nodeService: NodeService,
) : ViewModel() {

    private val viewModelJob = Job()
    private val vmScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    val stateID : StateID = StateID.fromId(encodedState)

    private var _transferId = MutableLiveData<Long>().apply {
        this.value = 0L
    }
    val transferId: MutableLiveData<Long>
        get() = _transferId

    var transfer: LiveData<RTransfer?>? = null

    private val _success = MutableLiveData<RTreeNode?>().apply {
        this.value = null
    }
    val success: LiveData<RTreeNode?>
        get() = _success

    // Manage UI
    private var _isProcessing = true
    val isProcessing: Boolean
        get() = _isProcessing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun cancelDownload() {
        vmScope.launch {
            transferId.value?.let {
                transferService.cancelTransfer(stateID.account(), it, AppNames.JOB_OWNER_USER)
            }
            _isProcessing = false
        }
    }

    fun launchDownload() {
        vmScope.launch {
            // TODO handle case when a download for the same file is already running
            val (jobId, errMsg) = transferService.prepareDownload(
                stateID, AppNames.LOCAL_FILE_TYPE_FILE, null
            )
            if (errMsg != null) {
                _isProcessing = false
                _errorMessage.value = errMsg
            } else {
                transfer = transferService.liveTransfer(stateID.account(), jobId)
                transferId.value = jobId
                transferService.runDownloadTransfer(stateID.account(), jobId, null)?.let {
                    _errorMessage.value = it
                    _isProcessing = false
                    return@launch
                }
                _success.value = nodeService.getLocalNode(stateID)
            }
        }
    }
}
