package com.pydio.android.cells.ui.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.utils.externallyView
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadVM(
    private val stateID: StateID,
    private val nodeService: NodeService,
    private val transferService: TransferService
) : ViewModel() {

    private val logTag = "DownloadVM"
    private val _rTreeNode = MutableStateFlow<RTreeNode?>(null)
    val treeNode: StateFlow<RTreeNode?> = _rTreeNode.asStateFlow()

    private val _transferID = MutableStateFlow(-1L)
    val transfer: LiveData<RTransfer?>
        get() = _transferID.asLiveData(viewModelScope.coroutineContext).switchMap { currID ->
            transferService.liveTransfer(stateID.account(), currID)
        }

    init {
        viewModelScope.launch {
            nodeService.getNode(stateID)?.let {
                _rTreeNode.value = it
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            if (_transferID.value >= 0) {
                transferService.cancelTransfer(stateID, _transferID.value, AppNames.JOB_OWNER_USER)
            }
        }
    }

    suspend fun launchDownload(): Boolean {
        val (transferID, errorMsg) =
            transferService.prepareDownload(stateID, AppNames.LOCAL_FILE_TYPE_FILE, null)
        if (!errorMsg.isNullOrBlank()) {
            Log.e(logTag, "Could not prepare download for $stateID: $errorMsg")
            return false
        }

        _transferID.value = transferID
        transferService.runDownloadTransfer(stateID.account(), transferID, null)?.let {
            Log.e(logTag, "Could not perform download for $stateID: $errorMsg")
            return false
        }
        // }
        return true
    }

    suspend fun viewFile(context: Context) {
        _rTreeNode.value?.let { node ->
            nodeService.getLocalFile(node, true)?.let { file ->
                externallyView(context, file, node)
                return
            } ?: run {
                throw SDKException(ErrorCodes.no_local_file)
            }
        }
    }
}
