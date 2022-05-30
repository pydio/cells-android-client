package com.pydio.android.cells.services

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTransferCancellation
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.childFile
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.FileNodeUtils
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class TransferService(
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val nodeService: NodeService,
    private val fileService: FileService,
) {

    private val logTag = TransferService::class.java.simpleName

    private val transferServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + transferServiceJob)

    companion object {
        // Hard-coded constants to ease implementation in a first pass. TODO: improve
        const val thumbDim = 300
        const val previewDim = 1024

        // The 2 below value are rough average for thumb and preview downloads
        const val thumbSize: Long = 20 * 1024
        const val previewSize: Long = 200 * 1024
    }

    private fun getTransferDao(accountId: StateID): TransferDao {
        return nodeDB(accountId).transferDao()
    }

    fun activeTransfers(stateId: StateID): LiveData<List<RTransfer>?> {
        return nodeDB(stateId).transferDao().getActiveTransfers()
    }

    fun liveTransfer(accountId: StateID, transferId: Long): LiveData<RTransfer?> {
        return nodeDB(accountId).transferDao().getLiveById(transferId)
    }

    fun enqueueUpload(parentID: StateID, uri: Uri) {
        val cr = CellsApp.instance.contentResolver
        serviceScope.launch {
            copyAndRegister(cr, uri, parentID)?.let {
                uploadOne(it)
            }
        }
    }

//    fun getLiveTransfer(stateId: StateID): LiveData<RTransfer?> {
//        return getTransferDao(stateId).getLiveByState(stateId.id)
//    }

    fun getLiveRecord(accountId: StateID, transferUid: Long): LiveData<RTransfer?> {
        return getTransferDao(accountId).getLiveById(transferUid)
    }

//    fun getTransferById(accountId: StateID, transferId: Long): RTransfer? {
//        return getTransferDao(accountId).getById(transferId)
//    }

    suspend fun clearTerminated(stateId: StateID) = withContext(Dispatchers.IO) {
        val dao = nodeDB(stateId).transferDao()
        dao.clearTerminatedTransfers()
        // We also remove unterminated jobs that have not been updated since more than 10 minutes
        dao.clearStaleTransfers(currentTimestamp() - 600)
    }

    suspend fun deleteRecord(stateId: StateID, transferId: Long) = withContext(Dispatchers.IO) {
        nodeDB(stateId).transferDao().deleteTransfer(transferId)
    }

    suspend fun cancelTransfer(stateId: StateID, transferId: Long, owner: String) =
        withContext(Dispatchers.IO) {
            val dao = nodeDB(stateId).transferDao()
            dao.insert(RTransferCancellation.cancel(transferId, stateId.id, owner))
        }

    /** DOWNLOADS **/

    suspend fun getFileForDisplay(
        state: StateID,
        type: String,
        parentJob: RJob?
    ): Pair<File?, String?> =
        withContext(Dispatchers.IO) {
            val account = state.account()
            val nodeDB = treeNodeRepository.nodeDB(account)
            val rNode = nodeDB.treeNodeDao().getNode(state.id)
            if (rNode == null) {
                // No node found, aborting
                val errMsg = "No node found for $state, aborting $type DL"
                Log.e(logTag, errMsg)
                return@withContext null to errMsg
            }

            // Detect if file is there and still up to date
            // TODO handle case when we are not connected
            fileService.getLocalFile(state, rNode, type)?.let {
                return@withContext it to null
            }

            downloadFile(state, rNode, type, parentJob, null)
        }

    suspend fun getFileForDiff(
        state: StateID,
        type: String,
        parentJob: RJob?,
        progressChannel: Channel<Long>?
    ): Pair<File?, String?> =
        withContext(Dispatchers.IO) {
            val account = state.account()
            val nodeDB = treeNodeRepository.nodeDB(account)
            val rNode = nodeDB.treeNodeDao().getNode(state.id)
            if (rNode == null) {
                // No node found, aborting
                val errMsg = "No node found for $state, aborting $type DL"
                Log.e(logTag, errMsg)
                return@withContext null to errMsg
            }

            downloadFile(state, rNode, type, parentJob, progressChannel)
        }

    /**
     * Launch the effective download. No check is done anymore: we assume the caller has
     * done them before calling this method.
     */
    suspend fun downloadFile(
        state: StateID,
        rNode: RTreeNode,
        type: String,
        parentJob: RJob?,
        progressChannel: Channel<Long>?
    ): Pair<File?, String?> {

        val parentFolder = fileService.dataParentPath(state.account(), type)

        val filename = when (type) {
            AppNames.LOCAL_FILE_TYPE_THUMB,
            AppNames.LOCAL_FILE_TYPE_PREVIEW -> {
                val name = dlThumb(state, rNode, parentFolder, type)
                if (type == AppNames.LOCAL_FILE_TYPE_THUMB) {
                    progressChannel?.send(thumbSize)
                } else if (type == AppNames.LOCAL_FILE_TYPE_PREVIEW) {
                    progressChannel?.send(previewSize)
                }
                name
            }
            AppNames.LOCAL_FILE_TYPE_FILE -> {
                val (jobId, errorMsg) = prepareDownload(state, type, parentJob)
                if (Str.notEmpty(errorMsg)) {
                    val errMsg = "could not launch download for $state: $errorMsg"
                    Log.e(logTag, errMsg)
                    return null to errMsg
                }
                val errorMsg2 = runDownloadTransfer(state, jobId, progressChannel)
                if (Str.notEmpty(errorMsg2)) {
                    val errMsg = "could not download file for $state: $errorMsg"
                    Log.e(logTag, errMsg)
                    return null to errMsg
                }
                // filename is...  (Note that we remove the leading slash to comply with childFile method signature, see just below)
                state.path.substring(1)
            }
            else -> null
        }
        return filename?.let { childFile(parentFolder, filename) } to null
    }

    /**
     * Centralize client specific actions that should be done **before** launching
     * the real download.
     */
    suspend fun prepareDownload(
        state: StateID,
        type: String,
        parentJob: RJob?
    ): Pair<Long, String?> =
        withContext(Dispatchers.IO) {

            // Retrieve data and sanity check
            val rNode = nodeService.getNode(state)
            if (rNode == null) {
                // No node found, aborting
                val errorMessage = "No node found for $state, aborting file DL"
                Log.w(logTag, errorMessage)
                return@withContext Pair(-1, errorMessage)
            }

            val localPath = fileService.getLocalPathFromState(state, type)
            val rec = RTransfer.fromState(
                state.id,
                AppNames.TRANSFER_TYPE_DOWNLOAD,
                localPath,
                rNode.size,
                rNode.mime,
                parentJobId = parentJob?.jobId ?: 0L
            )
            return@withContext Pair(getTransferDao(state).insert(rec), null)
        }

    /**
     * Performs the real download for the pre-registered transfer record and update
     * both the RTreeNode and RTransfer records depending on the output status.
     */
    suspend fun runDownloadTransfer(
        accountId: StateID,
        transferId: Long,
        progressChannel: Channel<Long>?
    ): String? = withContext(Dispatchers.IO) {

        var errorMessage: String? = null

        val dao = getTransferDao(accountId)

        // Retrieve data and sanity check
        val rTransfer = dao.getById(transferId) ?: run {
            val msg = "No record found for $transferId, aborting file DL"
            Log.w(logTag, msg)
            return@withContext msg
        }

        val state = StateID.fromId(rTransfer.encodedState)
        val rNode = nodeService.getNode(state)
        if (rNode == null) {
            // No node found, aborting
            errorMessage = "No node found for $state, aborting file DL"
            Log.w(logTag, errorMessage)
            return@withContext errorMessage
        }

        // Prepare target file
        val targetFile = File(rTransfer.localPath)
        targetFile.parentFile?.mkdirs()

        var out: FileOutputStream? = null
        try {

            Log.d(logTag, "About to download file from $state")

            out = FileOutputStream(targetFile)

            // Mark the download as started
            rTransfer.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
            rTransfer.status = AppNames.JOB_STATUS_PROCESSING
            dao.update(rTransfer)

            // Real transfer
            accountService.getClient(state)
                .download(state.workspace, state.file, out) { progressL ->

                    // TODO also manage parent job cancellation

                    val cancelled = dao.hasBeenCancelled(rTransfer.transferId)?.let {
                        val msg = "Download cancelled by ${it.owner}"
                        rTransfer.status = AppNames.JOB_STATUS_CANCELLED
                        rTransfer.doneTimestamp = currentTimestamp()
                        rTransfer.error = msg
                        errorMessage = msg
                        true
                    } ?: false

                    rTransfer.progress += progressL
                    rTransfer.updateTimestamp = currentTimestamp()
                    dao.update(rTransfer)
                    serviceScope.launch {
                        progressChannel?.send(progressL)
                    }
                    cancelled
                }

            // Mark the download as done
            if (rTransfer.status == AppNames.JOB_STATUS_PROCESSING) {
                rTransfer.status = AppNames.JOB_STATUS_DONE
                rTransfer.doneTimestamp = currentTimestamp()
                rTransfer.updateTimestamp = currentTimestamp()
                rTransfer.error = null
                dao.update(rTransfer)

                val type = AppNames.LOCAL_FILE_TYPE_FILE
                fileService.registerLocalFile(state, rNode, type, targetFile)
                // TODO handle the case where the download duration is long enough to enable
//                    //   end-user to modify (or delete) the corresponding node before it is downloaded
//                    rNode.localFilePath = rTransfer.localPath
            } else {
                // At this point, if we had an error or a cancel, the target file is most probably corrupted.
                // (We began to stream in...) -> so we remove both the file and the reference in the LocalFile table
                fileService.unregisterLocalFile(state, AppNames.LOCAL_FILE_TYPE_FILE)
            }

            // FIXME do we still need to update the index?
            // nodeDB(state).treeNodeDao().update(rNode)

        } catch (se: SDKException) { // Could not retrieve file, failing silently for the end user
            errorMessage = "Could not download file for " + state + ": " + se.message
            se.printStackTrace()
        } catch (ioe: IOException) {
            // TODO Could not write the file in the local fs, we should notify the user
            errorMessage =
                "Could not write file for DL of $state to the local device: ${ioe.message}"
            ioe.printStackTrace()
        } finally {
            IoHelpers.closeQuietly(out)
        }
        if (Str.notEmpty(errorMessage)) {
            rTransfer.doneTimestamp = currentTimestamp()
            rTransfer.status = AppNames.JOB_STATUS_ERROR
            rTransfer.error = errorMessage
            dao.update(rTransfer)
            // Try to remove partly downloaded file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Log.e(logTag, errorMessage!!)
        }

        return@withContext errorMessage
    }

    private fun dlThumb(state: StateID, rNode: RTreeNode, parPath: String, type: String): String? {
        val node = FileNode()
        node.properties = rNode.properties
        node.meta = rNode.meta
        try {
            val client = accountService.getClient(state)

            val dim = when (type) {
                AppNames.LOCAL_FILE_TYPE_THUMB -> thumbDim
                else -> previewDim // AppNames.LOCAL_FILE_TYPE_PREVIEW
            }
            val filename = client.getThumbnail(state, node, File(parPath), dim)
            val targetFile = File(parPath + File.separator + filename)
            if (!client.isLegacy) {
                handleOrientation(rNode, targetFile.absolutePath)
            }

            fileService.registerLocalFile(state, rNode, type, targetFile)
            return filename
        } catch (e: java.lang.Exception) {
            Log.e(logTag, "could not get thumb for $state: ${e.message}")
            // At this point, if we had an error, the target file is most probably corrupted or missing
            fileService.unregisterLocalFile(state, type)
            return null
        }
    }

    /**
     * Pydio Cells generates thumbnails without including main image EXIF data.
     * So we must manually get the orientation and add it to the thumb to ease later
     * manipulation of the images.
     * Note that we cannot do this in the Java SDK layer to use convenient library
     * that are provided by the android platform.
     */
    private fun handleOrientation(rTreeNode: RTreeNode, absPath: String) {
        // EXIF DATA must be manually retrieved from main image and applied
        val exifInterface = ExifInterface(absPath)
        if (rTreeNode.meta.containsKey(SdkNames.NODE_PROPERTY_IMG_EXIF_ORIENTATION)) {
            var orientation = rTreeNode.meta[SdkNames.NODE_PROPERTY_IMG_EXIF_ORIENTATION] as String
            orientation = FileNodeUtils.extractJSONString(orientation)
            exifInterface.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                orientation
            )
            exifInterface.saveAttributes()
        }
    }

    /** UPLOADS **/

    private fun uploadOne(stateID: StateID) {
        val dao = getTransferDao(stateID)
        val uploadRecord = dao.getByState(stateID.id)
            ?: throw IllegalStateException("No transfer record found for $stateID, cannot upload")
        doUpload(dao, uploadRecord)
    }

    private fun doUpload(dao: TransferDao, transferRecord: RTransfer) {
        // Real upload in single part
        var inputStream: InputStream? = null
        try {
            // Mark the upload as started
            transferRecord.startTimestamp = Calendar.getInstance().timeInMillis / 1000L

            dao.update(transferRecord)
            val state = transferRecord.getStateId()
            val srcPath =
                fileService.getLocalPathFromState(state, AppNames.LOCAL_FILE_TYPE_FILE)
            inputStream = FileInputStream(File(srcPath))

            Log.d(logTag, "... About to upload file to $state")

            val parent = state.parent()
            accountService.getClient(state).upload(
                inputStream, transferRecord.byteSize,
                transferRecord.mime, parent.workspace, parent.file, state.fileName,
                true
            ) { progressL ->
                transferRecord.progress = progressL
                dao.update(transferRecord)
                false
            }

            transferRecord.error = null
            transferRecord.doneTimestamp = Calendar.getInstance().timeInMillis / 1000L
            // uploadRecord.progress = 100

        } catch (e: Exception) {
            // TODO manage errors correctly
            transferRecord.error = e.message
            e.printStackTrace()
        } finally {
            IoHelpers.closeQuietly(inputStream)
            transferRecord.doneTimestamp = Calendar.getInstance().timeInMillis / 1000L
            dao.update(transferRecord)
        }
    }

//    suspend fun uploadAllNew() {
//        serviceScope.launch {
//            val uploads = getTransferDao().getAllNew()
//            for (one in uploads) {
//                serviceScope.launch {
//                    doUpload(one)
//                }
//            }
//        }
//    }

    /**
     * Does all the dirty work to copy the file from the device to in-app folder
     * and register a new transfer record in the DB
     */
    private fun copyAndRegister(cr: ContentResolver, uri: Uri, parentID: StateID): StateID? {
        var name: String? = null
        // TODO rather throw an exception 5 lines below if we do not have a valid size
        var size: Long = 1
        cr.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
            size = cursor.getLong(sizeIndex)
        }
        name = name ?: uri.lastPathSegment!!
        if (Str.empty(name)) {
            return null
        }

        val filename = name!!

        // Mime Type
        val mime = cr.getType(uri) ?: SdkNames.NODE_MIME_DEFAULT
        Log.d(logTag, "Enqueuing upload for $filename, MIME: [$mime], size: $size")

        // TODO should we implement a "clean" of the extensions
        //   the code below wasn't good enough and led to files named e.g. "img.JPG.jpg".
        //   Rather doing nothing.
        /*mimeMap.getExtensionFromMimeType(mime)?.let {
            //   - retrieve file extension
            //   - only append if the extension seems to be invalid
            if (!filename.endsWith(it, true)) {
                name += ".$it"
            }
        }*/

        // TODO to by-pass permission issues, we make a local copy of the file to upload
        //   in Cells app storage
        val fs = fileService
        val targetStateID = createLocalState(parentID, name as String)
        val localPath = fs.getLocalPathFromState(targetStateID, AppNames.LOCAL_FILE_TYPE_FILE)
        val localFile = File(localPath)
        localFile.parentFile!!.mkdirs()

        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = cr.openInputStream(uri)
            outputStream = FileOutputStream(localFile)
            IoHelpers.pipeRead(inputStream, outputStream)
        } catch (ioe: IOException) {
            Log.e(logTag, "could not create local copy of $filename: ${ioe.message}")
            ioe.printStackTrace()
            return null
        } finally {
            IoHelpers.closeQuietly(inputStream)
            IoHelpers.closeQuietly(outputStream)
        }

        val rec = RTransfer.fromState(
            targetStateID.id,
            AppNames.TRANSFER_TYPE_UPLOAD,
            localPath,
            size,
            mime
        )
        nodeDB(parentID).transferDao().insert(rec)
        return targetStateID
    }

    /* Constants and helpers */
    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

    private fun createLocalState(parentID: StateID, name: String): StateID {
        val parentPath =
            fileService.getLocalPathFromState(parentID, AppNames.LOCAL_FILE_TYPE_FILE)
        var targetFile = File(File(parentPath), name)
        while (targetFile.exists()) {
            val newName = bumpFileVersion(targetFile.name)
            targetFile = File(targetFile.parentFile!!, newName)
        }
        return parentID.child(targetFile.name)
    }

    private fun bumpFileVersion(name: String): String {
        val index = name.lastIndexOf(".")
        return if (index == -1) {
            handleWOExt(name)
        } else {
            val newPrefix = handleWOExt(name.substring(0, index))
            newPrefix + name.substring(index, name.length)
        }
    }

    private fun handleWOExt(name: String): String {
        val index = name.lastIndexOf("-")
        return if (index == -1) {
            "${name}-2"
        } else {
            val suffix = name.substring(index + 1, name.length)
            if (isInt(suffix)) {
                val newPrefix = name.substring(0, index)
                val newSuffix = suffix.toInt() + 1
                "${newPrefix}-${newSuffix}"
            } else {
                "${name}-2"
            }
        }
    }

    private fun isInt(value: String): Boolean {
        return try {
            value.toInt()
            true
        } catch (ex: java.lang.Exception) {
            false
        }
    }
}
