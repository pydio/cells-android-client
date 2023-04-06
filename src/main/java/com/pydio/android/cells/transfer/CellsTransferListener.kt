package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.utils.currentTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CellsTransferListener(
    private val scope: CoroutineScope,
    private val dao: TransferDao,
    private val transferRecord: RTransfer
) : TransferListener {

    private val logTag = "CellsTransferListener"

    override fun onStateChanged(id: Int, state: TransferState?) {
        scope.launch {
            when (state) {
                TransferState.COMPLETED -> {
                    transferRecord.status = AppNames.JOB_STATUS_ERROR
                    transferRecord.doneTimestamp = currentTimestamp()
                    dao.update(transferRecord)
                }
                else -> {
                    Log.e(logTag, "... #$id - State changed: $state")
                }
            }
        }
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        scope.launch {
            transferRecord.progress = bytesCurrent
            transferRecord.status = AppNames.JOB_STATUS_PROCESSING
            dao.update(transferRecord)
        }
        Log.e(logTag, "... #$id - Progress: $bytesCurrent / $bytesTotal")
    }

    override fun onError(id: Int, e: java.lang.Exception) {
        Log.e(logTag, "... #$id - Error: ${e.message}")
        e.printStackTrace()
        scope.launch {
            transferRecord.status = AppNames.JOB_STATUS_ERROR
            transferRecord.doneTimestamp = currentTimestamp()
            transferRecord.error = e.message
            dao.update(transferRecord)
        }
    }
}
