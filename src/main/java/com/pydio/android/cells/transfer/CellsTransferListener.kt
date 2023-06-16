package com.pydio.android.cells.transfer

import android.text.Html
import android.util.Log
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.pydio.android.cells.JobStatus
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
import java.util.UUID

class CellsTransferListener(
    private val externalID: Int,
    private val transferDao: TransferDao,
    private val done: () -> Unit,
    private val parentJobProgress: Channel<Long>? = null,
) : TransferListener, KoinComponent {

    private val id: String = UUID.randomUUID().toString()
    private val logTag = "CellsTrList-${id.substring(30)}"

    // init {
    //     Log.e(logTag, "## Created")
    //     Thread.dumpStack()
    //     Log.e(logTag, "###### ")
    // }

//     private val logTag = "CellsTransferListener"

    private val coroutineService: CoroutineService by inject()
    private val ioScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    private val fileService: FileService by inject()

    private var alreadyTransferred = 0L

    override fun onStateChanged(id: Int, state: TransferState?) {
        ioScope.launch(context = ioDispatcher) {
            val transferRecord = getTransferRecord()
            when (state) {
                TransferState.COMPLETED -> {
                    Log.i(logTag, "... #$id - ${transferRecord.transferId}: Transfer complete")
                    fileService.registerLocalFile(transferRecord)
                    transferRecord.status = JobStatus.DONE.id
                    transferRecord.doneTimestamp = currentTimestamp()
                    transferDao.update(transferRecord)
                    done()
                }

                else -> {
                    // TODO handle pause and resume
                    // Log.e(logTag, "... #$id - State changed: $state")
                }
            }
        }
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        ioScope.launch {
            val transferRecord = getTransferRecord()
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
        val msg = if (e is AmazonS3Exception) {
            Log.e(logTag, "Could not transfer #$id - ${e.errorMessage}")
            // TODO perform a better parsing of the XML
            val parsed = try {
                val tmp = e.localizedMessage!!.subSequence(
                    e.localizedMessage!!.indexOf("<Message>") + "<Message>".length,
                    e.localizedMessage!!.indexOf("</Message>")
                ).toString()
                Html.fromHtml(tmp, Html.FROM_HTML_MODE_LEGACY).toString()
            } catch (e2: Exception) {
                Log.e(logTag, "Could not parse error ${e.errorMessage}: ${e2.message}")
                e.errorMessage ?: "Could not perform transfer"
            }
            Log.e(logTag, "Parsed Error ${e.errorCode}: $parsed")
            "${e.errorCode}: $parsed"
        } else {
            Log.e(logTag, "Unexpected Error for transfer #$id: ${e.message}")
            e.printStackTrace()
            e.message
        }

        ioScope.launch {
            try {
                val transferRecord = getTransferRecord()
                transferRecord.status = JobStatus.ERROR.id
                transferRecord.doneTimestamp = currentTimestamp()
                transferRecord.error = msg
                transferDao.update(transferRecord)
            } catch (se: SDKException) {
                Log.e(logTag, "Could not put transfer in error: ${se.code} -${se.message}  ")
                se.printStackTrace()
            }
        }
    }

    private fun getTransferRecord(): RTransfer = transferDao.getByExternalID(externalID) ?: run {
        throw SDKException(
            ErrorCodes.illegal_argument,
            "Could not retrieve transfer with external ID $externalID, aborting"
        )
    }
}
