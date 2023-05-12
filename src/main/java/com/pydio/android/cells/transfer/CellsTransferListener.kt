package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CellsTransferListener(
    private val externalID: Int,
    private val transferDao: TransferDao,
    private val parentJobProgress: Channel<Long>? = null
) : TransferListener, KoinComponent {

    private val logTag = "CellsTransferListener"

    private val coroutineService: CoroutineService by inject()
    private val scope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    private val fileService: FileService by inject()

    // We rely on an instance object to avoid race conditions.
    // It is still a bit clumsy and we must pay attention to only update distinct
    // fields of the record in the various methods.
    private val transferRecord: RTransfer = transferDao.getByExternalID(externalID) ?: run {
        throw SDKException(
            ErrorCodes.internal_error,
            "Could not retrieve transfer with external ID $externalID, aborting"
        )
    }
    private var alreadyTransferred = 0L

    override fun onStateChanged(id: Int, state: TransferState?) {
        scope.launch(context = ioDispatcher) {
            when (state) {
                TransferState.COMPLETED -> {
                    Log.i(logTag, "... #$id - ${transferRecord.transferId}: Transfer complete")
                    fileService.registerLocalFile(transferRecord)
                    transferRecord.status = AppNames.JOB_STATUS_DONE
                    transferRecord.doneTimestamp = currentTimestamp()
                    transferDao.update(transferRecord)
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
                transferDao.update(transferRecord)

                parentJobProgress?.let {
                    val diff = bytesCurrent - alreadyTransferred
                    if (diff > 0) {
                        try {
                            it.send(diff)
                        } catch (ce: ClosedSendChannelException) {
                            Log.e(
                                logTag,
                                "... Could not update progress was: $bytesCurrent / $bytesTotal, cause: ${ce.message ?: "-"}"
                            )

                        }
                    }
                    alreadyTransferred = bytesCurrent
                }
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
            transferDao.update(transferRecord)
        }
    }
}
