package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CellsTransferListener(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val dao: TransferDao,
    private val externalID: Int
) : TransferListener {

    private val logTag = "CellsTransferListener"

    // We rely on an instance object to avoid race conditions.
    // It is still a bit clumsy and we must pay attention to only update distinct
    // fields of the record in the various methods.
    private val transferRecord: RTransfer = dao.getByExternalID(externalID) ?: run {
        throw SDKException(
            ErrorCodes.internal_error,
            "Could not retrieve transfer with external ID $externalID, aborting"
        )
    }

    override fun onStateChanged(id: Int, state: TransferState?) {
        scope.launch(context = ioDispatcher) {
            when (state) {
                TransferState.COMPLETED -> {
                    Log.i(logTag, "... #$id - ${transferRecord.transferId}: Transfer complete")
                    transferRecord.status = AppNames.JOB_STATUS_DONE
                    transferRecord.doneTimestamp = currentTimestamp()
                    dao.update(transferRecord)
                }
                else -> {
                    // TODO handle pause and resume
                    // Log.e(logTag, "... #$id - State changed: $state")
                }
            }
        }
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        scope.launch(context = ioDispatcher) {
            if (bytesCurrent != transferRecord.progress) {
                Log.d(logTag, "... #$id - Progress: $bytesCurrent / $bytesTotal")
                transferRecord.progress = bytesCurrent
                dao.update(transferRecord)
//            } else {
//                Log.e(logTag, "... #$id - Progress is unchanged")
            }
        }
    }

    override fun onError(id: Int, e: java.lang.Exception) {
        Log.e(logTag, "... #$id - Error: ${e.message}")
        e.printStackTrace()
        scope.launch(context = ioDispatcher) {
            transferRecord.status = AppNames.JOB_STATUS_ERROR
            transferRecord.doneTimestamp = currentTimestamp()
            transferRecord.error = e.message
            dao.update(transferRecord)
        }
    }
}
