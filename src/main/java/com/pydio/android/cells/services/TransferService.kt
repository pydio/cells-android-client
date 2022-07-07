package com.pydio.android.cells.services

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTransferCancellation
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.childFile
import com.pydio.android.cells.utils.computeFileMd5
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.decodeSortById
import com.pydio.cells.api.ErrorCodes
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
import kotlinx.coroutines.delay
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
    private val prefs: CellsPreferences,
    private val networkService: NetworkService,
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

    fun queryTransfers(stateId: StateID): LiveData<List<RTransfer>> {
        val filterByStatus = prefs.getString(
            AppKeys.TRANSFER_FILTER_BY_STATUS, AppNames.JOB_STATUS_NO_FILTER
        )
        return queryTransfersExplicitFilter(stateId, filterByStatus)
    }

    private fun queryTransfersExplicitFilter(
        stateId: StateID,
        filterByStatus: String
    ): LiveData<List<RTransfer>> {
        val (sortByCol, sortByOrder) = decodeSortById(
            prefs.getString(
                AppKeys.TRANSFER_SORT_BY, AppNames.JOB_SORT_BY_DEFAULT
            )
        )
        val lsQuery = if (filterByStatus == AppNames.JOB_STATUS_NO_FILTER) {
            SimpleSQLiteQuery("SELECT * FROM transfers ORDER BY $sortByCol $sortByOrder")
        } else {
            SimpleSQLiteQuery(
                "SELECT * FROM transfers WHERE status = ? ORDER BY $sortByCol $sortByOrder ",
                arrayOf(filterByStatus)
            )
        }
        // Log.e(logTag, "About to query: ${lsQuery.sql}, with ${lsQuery.argCount} arg")
        return nodeDB(stateId).transferDao().transferQuery(lsQuery)
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

    fun getLiveRecord(accountId: StateID, transferUid: Long): LiveData<RTransfer?> {
        return getTransferDao(accountId).getLiveById(transferUid)
    }

    suspend fun clearTerminated(stateId: StateID) = withContext(Dispatchers.IO) {
        val dao = nodeDB(stateId).transferDao()
        // val before = dao.getTransferCount()

        dao.clearTerminatedTransfers()
        // We also remove unterminated jobs that have not been updated since more than 10 minutes
        dao.clearStaleTransfers(currentTimestamp() - 600)
        // val after = dao.getTransferCount()
        // Log.e(logTag, "After transfer clean: $after (B4: $before)")
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

            // Various cases depending on local file presence, network status and user preferences
            fileService.getLocalFile(state, rNode, type)?.let {
                return@withContext it to null
            }

            if (networkService.isConnected() && !networkService.isMetered()) {
                return@withContext downloadFile(state, rNode, type, parentJob, null)
            }

            val dlThumbOnMetered = prefs.getBoolean(AppKeys.METERED_DL_THUMBS, false)
            if (dlThumbOnMetered && (type == AppNames.LOCAL_FILE_TYPE_THUMB ||
                        type == AppNames.LOCAL_FILE_TYPE_PREVIEW)
            ) {
                return@withContext downloadFile(state, rNode, type, parentJob, null)
            }

            val dlFileOnMetered = prefs.getBoolean(AppKeys.METERED_ASK_B4_DL_FILES, false)
            if (dlFileOnMetered && (type == AppNames.LOCAL_FILE_TYPE_FILE)
            ) {
                return@withContext downloadFile(state, rNode, type, parentJob, null)
            }

            throw SDKException(ErrorCodes.con_failed, "could not dl $type for $state")
            // return@withContext null to "could not dl $type for $state"
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
    private suspend fun downloadFile(
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
                    Log.e(logTag, errorMsg2!!)
                    return null to errorMsg2
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
            val rNode = nodeService.getLocalNode(state)
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
        val lfType = AppNames.LOCAL_FILE_TYPE_FILE


        // Retrieve data and sanity check
        val rTransfer = dao.getById(transferId) ?: run {
            val msg = "No record found for $transferId, aborting file DL"
            Log.w(logTag, msg)
            return@withContext msg
        }

        val state = StateID.fromId(rTransfer.encodedState)
        val rNode = nodeService.getLocalNode(state)
        if (rNode == null) {
            // No node found, aborting
            errorMessage = "No node found for $state, aborting file DL"
            Log.w(logTag, errorMessage)
            return@withContext errorMessage
        }

        // TODO Insure we are connected and not metered (or DL with metered is allowed in prefs)

        Log.d(logTag, "About to download file from $state")
        // Prepare target file
        val targetFile = File(rTransfer.localPath)
        targetFile.parentFile?.mkdirs()
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(targetFile)

            // Mark the download as started
            rTransfer.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
            rTransfer.status = AppNames.JOB_STATUS_PROCESSING
            dao.update(rTransfer)

            // Real transfer
            var lastUpdateTS = 0L
            var byteWritten = 0L
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

                    byteWritten += progressL
                    val newTs = currentTimestamp()

                    // We only update the records every seconds)
                    if (newTs - lastUpdateTS >= 1) {
                        rTransfer.progress += byteWritten
                        rTransfer.updateTimestamp = newTs
                        dao.update(rTransfer)
                        val increment = byteWritten
                        serviceScope.launch {
                            progressChannel?.send(increment)
                        }
                        byteWritten = 0
                        lastUpdateTS = rTransfer.updateTimestamp
                    }
                    cancelled
                }

            if (rTransfer.status == AppNames.JOB_STATUS_PROCESSING) {
                // Mark the download as done
                if (byteWritten > 0) {
                    serviceScope.launch {
                        progressChannel?.send(byteWritten)
                    }
                }
                rTransfer.progress += byteWritten
                rTransfer.updateTimestamp = currentTimestamp()
                dao.update(rTransfer)

                // Double check downloaded file is OK
                val computedMd5 = computeFileMd5(targetFile)
                if (rNode.etag != computedMd5) {
                    rTransfer.error = "MD5 signatures do not match after the download has terminated"
                    rTransfer.status = AppNames.JOB_STATUS_WARNING
                } else {
                    rTransfer.status = AppNames.JOB_STATUS_DONE
                    rTransfer.error = null
                }
                fileService.registerLocalFile(state, rNode, lfType, targetFile)
                rTransfer.doneTimestamp = currentTimestamp()
                rTransfer.updateTimestamp = currentTimestamp()
                dao.update(rTransfer)

                // TODO handle the case where the download duration is long enough to enable
                //   end-user to modify (or delete) the corresponding node before it has been correctly downloaded
            }

        } catch (se: SDKException) { // Could not retrieve file, failing silently for the end user
            errorMessage = "could not download file for " + state + ": " + se.message
            se.printStackTrace()
        } catch (ioe: IOException) {
            // TODO Could not write the file in the local fs, we should notify the user
            errorMessage =
                "could not write file for DL of $state to the local device: ${ioe.message}"
            ioe.printStackTrace()
        } finally {
            IoHelpers.closeQuietly(out)
        }
        if (Str.notEmpty(errorMessage)) {
            rTransfer.doneTimestamp = currentTimestamp()
            rTransfer.status = AppNames.JOB_STATUS_ERROR
            rTransfer.error = errorMessage
            dao.update(rTransfer)

            // At this point, if we had an error or a cancel, the target file is most probably corrupted.
            // (We began to stream in...) -> so we remove both the file and the reference in the LocalFile table
            // Try to remove partly downloaded file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            // And unregister local file record
            fileService.unregisterLocalFile(state, lfType)
            Log.e(logTag, "Could not download file at $state : $errorMessage")
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
            if (Str.empty(filename)) {
                Log.e(logTag, "could not get thumb for $state but no error have been thrown")
                return null
            }

            val targetFile = File(parPath + File.separator + filename)
            if (!client.isLegacy) {
                handleOrientation(rNode, targetFile.absolutePath)
            }

            fileService.registerLocalFile(state, rNode, type, targetFile)
            return filename
        } catch (e: java.lang.Exception) {
            Log.e(logTag, "could not get thumb for $state: ${e.message}")
            // TODO improve At this point, if we had an error, the target file is most probably corrupted or missing
            //   Problem: if we are offline we might reach this point and and remove the record too fast. 
            // e.printStackTrace()
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
                transferRecord.status = AppNames.JOB_STATUS_PROCESSING
                transferRecord.updateTimestamp = currentTimestamp()
                dao.update(transferRecord)
                false
            }

            transferRecord.error = null
            transferRecord.doneTimestamp = currentTimestamp()
            transferRecord.status = AppNames.JOB_STATUS_DONE

        } catch (e: Exception) {
            // TODO manage errors correctly
            transferRecord.error = e.message
            transferRecord.status = AppNames.JOB_STATUS_ERROR
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

    /**
     * Debug method to easily debug transfers live Data issues
     */
    suspend fun createDummyTransfers(accountId: StateID) = withContext(Dispatchers.IO) {
        val dao = getTransferDao(accountId)
        var i = 0
        while (i < 10000) {

            if (dao.getTransferCount() < 20) {

                Log.e(logTag, "Creating job with id $i")
                val recInit = RTransfer.fromState(
                    accountId.withPath("/common-files/dummy/$i").id,
                    AppNames.TRANSFER_TYPE_UPLOAD,
                    "/data/user/common-files/dummy/$i",
                    1024L * 1024L,
                    SdkNames.NODE_MIME_DEFAULT
                )

                val newJobId = dao.insert(recInit)

                val rec = dao.getById(newJobId) ?: continue

                // start dummy transfer
                rec.startTimestamp = currentTimestamp()
                rec.status = AppNames.JOB_STATUS_PROCESSING
                dao.update(rec)

                // Dummy transfer with progress
                while (rec.progress < rec.byteSize) {
                    rec.progress += 10240
                    rec.updateTimestamp = currentTimestamp()
                    dao.update(rec)
                    delay(100)
                }

                // Terminate
                rec.updateTimestamp = currentTimestamp()
                rec.error = null
                rec.status = AppNames.JOB_STATUS_DONE
                rec.doneTimestamp = rec.updateTimestamp
                dao.update(rec)
            }
            delay(2000)
            i++
        }
    }


}
