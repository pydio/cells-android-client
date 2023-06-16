package com.pydio.android.cells.ui.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.ui.core.AbstractCellsVM
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class DownloadVM(
    val isRemoteLegacy: Boolean,
    private val stateID: StateID,
    private val transferService: TransferService
) : AbstractCellsVM() {

    private val logTag = "DownloadVM"
    private val _rTreeNode = MutableStateFlow<RTreeNode?>(null)
    val treeNode: StateFlow<RTreeNode?> = _rTreeNode.asStateFlow()

    private val _transferID = MutableStateFlow(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transfer: Flow<RTransfer?> = _transferID.flatMapLatest { currID ->
        try {
            transferService.liveTransfer(stateID.account(), currID)
        } catch (ie: IllegalArgumentException) {
            Log.e(logTag, "Cannot get live transfer with TID  $currID: ${ie.message}")
            flow { }
        } catch (e: Exception) {
            Log.e(logTag, "Unexpected error for TID: $currID: ${e.message}")
            flow { }
        }
    }

    suspend fun viewFile(context: Context) {
        viewFile(context, stateID)
    }

    suspend fun launchDownload() {
        try {
            transferService.currentDownload(stateID)?.let {
                showError(ErrorMessage("No need to relaunch", -1, listOf()))
                return
            }
            val transferID = transferService.prepareDownload(stateID, AppNames.LOCAL_FILE_TYPE_FILE)
            _transferID.value = transferID
            transferService.runDownloadTransfer(stateID.account(), transferID, null)
        } catch (se: SDKException) {
            val msg = "Cannot download file for $stateID"
            Log.e(logTag, "$msg, cause: ${se.message ?: "-"} ")
            showError(ErrorMessage(msg, -1, listOf()))
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            if (_transferID.value >= 0) {
                try {
                    transferService.cancelTransfer(
                        stateID,
                        _transferID.value,
                        AppNames.JOB_OWNER_USER,
                        isRemoteLegacy
                    )
                } catch (e: Exception) {
                    done(e)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            nodeService.getNode(stateID)?.let {
                _rTreeNode.value = it
            }
        }
    }
}
