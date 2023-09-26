package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTransferCancellation
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.utils.computeFileMd5
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Calendar

class P8TransferService(
    coroutineService: CoroutineService,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val fileService: FileService,
) {

    private val logTag = "P8TransferService"

    private val ioScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    @Suppress("BlockingMethodInNonBlockingContext")
    @Throws(SDKException::class)
    suspend fun doDownload(
        stateID: StateID,
        targetFile: File,
        parentJobProgress: Channel<Long>?,
        dao: TransferDao,
        rTreeNode: RTreeNode,
        rTransfer: RTransfer
    ) = withContext(ioDispatcher) {

        val lfType = AppNames.LOCAL_FILE_TYPE_FILE

        var out: FileOutputStream? = null
        var exception: SDKException? = null
        try {
            out = FileOutputStream(targetFile)

            // Mark the download as started
            rTransfer.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
            rTransfer.status = JobStatus.PROCESSING.id
            dao.update(rTransfer)

            // Real transfer
            var lastUpdateTS = 0L
            var byteWritten = 0L
            accountService.getClient(stateID)
                .download(stateID.slug, stateID.file, out) { progressL ->
                    // TODO also manage parent job cancellation
                    val cancellationMsg = dao.hasBeenCancelled(rTransfer.transferId)?.let {
                        val msg = "Download paused by ${it.owner}"
                        rTransfer.status = JobStatus.CANCELLED.id
                        rTransfer.doneTimestamp = currentTimestamp()
                        rTransfer.error = msg
                        // TODO register a cancellation exception ?
//                        errorMessage = msg
                        msg
                    } ?: ""

                    byteWritten += progressL
                    val newTs = currentTimestamp()

                    // We only update the records every seconds)
                    if (newTs - lastUpdateTS >= 1) {
                        rTransfer.progress += byteWritten
                        rTransfer.updateTimestamp = newTs
                        dao.update(rTransfer)
                        val increment = byteWritten
                        ioScope.launch {
                            parentJobProgress?.send(increment)
                        }
                        byteWritten = 0
                        lastUpdateTS = rTransfer.updateTimestamp
                    }
                    cancellationMsg
                }

            if (rTransfer.status == JobStatus.PROCESSING.id) {
                // Mark the download as done
                if (byteWritten > 0) {
                    ioScope.launch {
                        parentJobProgress?.send(byteWritten)
                    }
                }
                rTransfer.progress += byteWritten
                rTransfer.updateTimestamp = currentTimestamp()
                dao.update(rTransfer)

                // Double check downloaded file is OK, skip for P8
                if (rTreeNode.etag != null) {
                    val computedMd5 = computeFileMd5(targetFile)
                    if (rTreeNode.etag != computedMd5) {
                        rTransfer.error =
                            "MD5 signatures do not match after the download has terminated"
                        rTransfer.status = JobStatus.WARNING.id
                    } else {
                        rTransfer.status = JobStatus.DONE.id
                        rTransfer.error = null
                    }
                } else { // No check for P8
                    rTransfer.status = JobStatus.DONE.id
                    rTransfer.error = null
                }
                fileService.registerLocalFile(stateID, rTreeNode, lfType, targetFile)
                rTransfer.doneTimestamp = currentTimestamp()
                rTransfer.updateTimestamp = currentTimestamp()
                dao.update(rTransfer)
                // TODO handle the case where the download duration is long enough to enable
                //   end-user to modify (or delete) the corresponding node before it has been correctly downloaded
            }
        } catch (se: SDKException) { // Could not retrieve file, failing silently for the end user
            val errMsg = "could not download file for $stateID"
            exception = SDKException(se.code, errMsg, se)
        } catch (ioe: IOException) {
            val errMsg = "could not write file for $stateID to local app folder"
            exception = SDKException(ErrorCodes.local_io_error, errMsg, ioe)
        } finally {
            IoHelpers.closeQuietly(out)
        }
        exception?.let {

            rTransfer.doneTimestamp = currentTimestamp()
            rTransfer.status = JobStatus.ERROR.id
            rTransfer.error = "${exception.message}, cause: ${exception.cause?.message ?: "-"}"
            dao.update(rTransfer)

            // At this point, if we had an error or a cancel, the target file is most probably corrupted.
            // (We began to stream in...) -> so we remove both the file and the reference in the LocalFile table
            // Try to remove partly downloaded file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            // And unregister local file record
            fileService.unregisterLocalFile(stateID, lfType)

            // And finally rethrow the exception
            throw exception
            // Log.e(logTag, "Could not download file at $stateID : $errorMessage")
        }
    }

    suspend fun doUpload(
        stateID: StateID,
        srcFile: File,
        dao: TransferDao,
        transferRecord: RTransfer
    ) = withContext(ioDispatcher) {
        dao.ackCancellation(transferRecord.transferId)
        var inputStream: InputStream? = null
        var cancellationMsg: String
        var lastUpdateTS = 0L
        var byteWritten = 0L

        val parentID = stateID.parent()

        try {
            inputStream = FileInputStream(srcFile)
            dao.update(transferRecord)

            accountService.getClient(stateID).upload(
                inputStream, transferRecord.byteSize,
                transferRecord.mime, parentID.slug, parentID.file, stateID.fileName,
                true
            ) { progressL ->

                byteWritten += progressL

                cancellationMsg = dao.hasBeenCancelled(transferRecord.transferId)?.let {
                    val msg = "Upload cancelled by ${it.owner}"
                    transferRecord.status = JobStatus.CANCELLED.id
                    transferRecord.error = msg
                    Log.w(logTag, "### Cancel requested: $msg")
                    // We also reset the number of bytes sent, relaunching always triggers a full upload.
                    transferRecord.progress = 0
                    msg
                } ?: ""

                // We only update the records every half second and when the upload is not cancelled)
                val newTs = System.currentTimeMillis()
                if (Str.empty(cancellationMsg) && newTs - lastUpdateTS >= 500) {
                    Log.d(logTag, "- Transfer $byteWritten / ${transferRecord.byteSize}")
                    transferRecord.progress += byteWritten
                    transferRecord.updateTimestamp = newTs
                    transferRecord.status = JobStatus.PROCESSING.id
                    dao.update(transferRecord)
                    byteWritten = 0
                    lastUpdateTS = newTs
                }
                cancellationMsg
            }
            Log.i(logTag, "### Done and not cancelled")
            transferRecord.error = null
            transferRecord.doneTimestamp = currentTimestamp()
            transferRecord.status = JobStatus.DONE.id
            // Also send remaining bits to the progress bar
            transferRecord.progress += byteWritten
            Log.e(logTag, "... ${transferRecord.progress} / ${transferRecord.byteSize}")
        } finally {
            IoHelpers.closeQuietly(inputStream)
        }
    }

    suspend fun cancelTransfer(stateID: StateID, transferID: Long, owner: String) =
        withContext(ioDispatcher) {
            val dao = nodeDB(stateID).transferDao()
            dao.insert(RTransferCancellation.cancel(transferID, stateID.id, owner))
        }

    fun pauseTransfer(stateID: StateID, transferID: Long, owner: String) {
        throw IllegalStateException(
            "Cannot pause transfer when remote is a P8 server, " +
                    "params: StateID: $stateID, transferID: $transferID, owner: $owner"
        )
    }

    fun resumeTransfer(stateID: StateID, transferID: Long) {
        throw IllegalStateException(
            "Cannot resume transfer when remote is a P8 server, " +
                    "params: StateID: $stateID, transferID: $transferID"
        )
    }

    suspend fun forgetTransfer(stateID: StateID, transferID: Long) =
        withContext(ioDispatcher) {
            nodeDB(stateID).transferDao().deleteTransfer(transferID)
        }


    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

}
