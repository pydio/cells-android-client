package com.pydio.android.cells.services

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTransferCancellation
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.reactive.NetworkStatus
import com.pydio.android.cells.transfer.CellsS3Client
import com.pydio.android.cells.transfer.CellsTransferListener
import com.pydio.android.cells.transfer.getTransferUtility
import com.pydio.android.cells.utils.childFile
import com.pydio.android.cells.utils.computeFileMd5
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.parseOrder
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
import kotlinx.coroutines.cancel
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
    private val prefs: PreferencesService,
    private val networkService: NetworkService,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val nodeService: NodeService,
    private val fileService: FileService,
) {

    private val logTag = "TransferService"

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

    fun getTransfersRecordsForJob(
        accountID: StateID,
        jobID: Long
    ): LiveData<List<RTransfer>> {
        val id =
            if (jobID < 1) -2 else jobID // tweak to insure we return no jobs when no job ID has been explicitly set
        return nodeDB(accountID).transferDao().getByJobId(id)
    }

    fun getCurrentTransfersRecords(
        accountID: StateID,
        transferIDs: Set<Long>
    ): LiveData<List<RTransfer>> {
        return nodeDB(accountID).transferDao().getCurrents(transferIDs)
    }

    fun queryTransfersExplicitFilter(
        stateID: StateID,
        filterByStatus: String,
        encodedOrder: String,
    ): LiveData<List<RTransfer>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedOrder, ListType.TRANSFER)
        val lsQuery = if (filterByStatus == AppNames.JOB_STATUS_NO_FILTER) {
            SimpleSQLiteQuery("SELECT * FROM transfers ORDER BY $sortByCol $sortByOrder")
        } else {
            SimpleSQLiteQuery(
                "SELECT * FROM transfers WHERE status = ? ORDER BY $sortByCol $sortByOrder ",
                arrayOf(filterByStatus)
            )
        }
        // Log.e(logTag, "About to query: ${lsQuery.sql}, with ${lsQuery.argCount} arg")
        return nodeDB(stateID).transferDao().transferQuery(lsQuery)
    }

    fun liveTransfer(accountID: StateID, transferID: Long): LiveData<RTransfer?> {
        return nodeDB(accountID).transferDao().getLiveById(transferID)
    }

    fun enqueueUpload(parentID: StateID, uri: Uri) {
        val cr = CellsApp.instance.contentResolver
        serviceScope.launch {
            val tid = register(cr, uri, parentID)
            launchCopy(cr, uri, parentID, tid.first, tid.second)?.let {
                launch {
                    try {
                        uploadOne(it)
                        Log.w(logTag, "... $it ==> upload DONE")
                    } catch (e: Exception) {
                        Log.e(logTag, "... $it ==> upload FAILED: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    suspend fun getRecord(accountID: StateID, transferID: Long): RTransfer? =
        withContext(Dispatchers.IO) {
            return@withContext getTransferDao(accountID).getById(transferID)
        }

    suspend fun clearTerminated(stateID: StateID) = withContext(Dispatchers.IO) {
        val dao = nodeDB(stateID).transferDao()
        // val before = dao.getTransferCount()

        dao.clearTerminatedTransfers()
        // We also remove unterminated jobs that have not been updated since more than 10 minutes
        dao.clearStaleTransfers(currentTimestamp() - 600)
        // val after = dao.getTransferCount()
        // Log.e(logTag, "After transfer clean: $after (B4: $before)")
    }

    suspend fun deleteRecord(stateID: StateID, transferID: Long) = withContext(Dispatchers.IO) {
        nodeDB(stateID).transferDao().deleteTransfer(transferID)
    }

    suspend fun cancelTransfer(stateId: StateID, transferID: Long, owner: String) =
        withContext(Dispatchers.IO) {
            val dao = nodeDB(stateId).transferDao()
            dao.insert(RTransferCancellation.cancel(transferID, stateId.id, owner))
        }

    /** DOWNLOADS **/
    suspend fun getImageForDisplay(
        stateID: StateID,
        type: String,
        parentJob: RJob?
    ): Pair<File?, String?> =
        withContext(Dispatchers.IO) {
            val accountID = stateID.account()
            val nodeDB = treeNodeRepository.nodeDB(accountID)
            val rNode = nodeDB.treeNodeDao().getNode(stateID.id)
            if (rNode == null) {
                // No node found, aborting
                val errMsg = "No node found for $stateID, aborting $type DL"
                Log.e(logTag, errMsg)
                return@withContext null to errMsg
            }

            // First try to retrieve local file
            fileService.getLocalFile(stateID, rNode, type)?.let {
                return@withContext it to null
            }

            // Otherwise, try to download if network type and user preferences allow it
            val currSettings = prefs.fetchPreferences()
//            val applyLimit = prefs.getBoolean(AppKeys.APPLY_METERED_LIMITATION, true)
//            val dlThumb = prefs.getBoolean(AppKeys.METERED_DL_THUMBS, false)
            when (networkService.networkStatus) {
                is NetworkStatus.Unmetered
                -> return@withContext downloadFile(stateID, rNode, type, parentJob, null)
                is NetworkStatus.Metered
                -> if (!currSettings.meteredNetwork.applyLimits || currSettings.meteredNetwork.dlThumbs) {
                    return@withContext downloadFile(stateID, rNode, type, parentJob, null)
                } else {
                    return@withContext null to "Cannot download preview images on metered network"
                }
                else -> return@withContext null to "No network connection: cannot download preview image"

            }
//            throw SDKException(ErrorCodes.con_failed, "could not dl $type for $state")
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
                val (name, errorMsg) = dlThumb(state, rNode, parentFolder, type)
                if (Str.notEmpty(errorMsg)) {
                    return null to errorMsg
                }
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
        if (filename == null) {
            // FIXME why is this empty 
        }
        return filename?.let { childFile(parentFolder, filename) } to null
    }

    /**
     * Centralize client specific actions that should be done **before** launching
     * the real download.
     */
    suspend fun prepareDownload(
        stateID: StateID,
        type: String,
        parentJob: RJob?
    ): Pair<Long, String?> =
        withContext(Dispatchers.IO) {

            // Retrieve data and sanity check
            val rNode = nodeService.getNode(stateID)
            if (rNode == null) {
                // No node found, aborting
                val errorMessage = "No node found for $stateID, aborting file DL"
                Log.w(logTag, errorMessage)
                return@withContext Pair(-1, errorMessage)
            }

            val localPath = fileService.getLocalPathFromState(stateID, type)
            val rec = RTransfer.fromState(
                stateID.id,
                AppNames.TRANSFER_TYPE_DOWNLOAD,
                localPath,
                rNode.size,
                rNode.mime,
                parentJobId = parentJob?.jobId ?: 0L
            )
            val transferID = getTransferDao(stateID).insert(rec)
            Log.e(logTag, "Prepared transfer #$transferID for $stateID")
            return@withContext transferID to null
        }

    /**
     * Performs the real download for the pre-registered transfer record and update
     * both the RTreeNode and RTransfer records depending on the output status.
     */
    suspend fun runDownloadTransfer(
        accountID: StateID,
        transferID: Long,
        progressChannel: Channel<Long>?
    ): String? = withContext(Dispatchers.IO) {

        var errorMessage: String? = null
        val dao = getTransferDao(accountID)
        val lfType = AppNames.LOCAL_FILE_TYPE_FILE

        // Retrieve data and sanity check
        val rTransfer = dao.getById(transferID) ?: run {
            val msg = "No record found for $transferID, aborting file DL"
            Log.w(logTag, msg)
            return@withContext msg
        }

        val localPath = rTransfer.localPath ?: run {
            val msg = "No local path is defined for $transferID, aborting file DL"
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

        Log.d(logTag, "About to download file from $state")
        // Prepare target file
        val targetFile = File(localPath)
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
                    val cancellationMsg = dao.hasBeenCancelled(rTransfer.transferId)?.let {
                        val msg = "Download cancelled by ${it.owner}"
                        rTransfer.status = AppNames.JOB_STATUS_CANCELLED
                        rTransfer.doneTimestamp = currentTimestamp()
                        rTransfer.error = msg
                        errorMessage = msg
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
                        serviceScope.launch {
                            progressChannel?.send(increment)
                        }
                        byteWritten = 0
                        lastUpdateTS = rTransfer.updateTimestamp
                    }
                    cancellationMsg
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
                    rTransfer.error =
                        "MD5 signatures do not match after the download has terminated"
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

    private fun dlThumb(
        state: StateID,
        rNode: RTreeNode,
        parPath: String,
        type: String
    ): Pair<String?, String?> {
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
                return null to "Could not get thumb for $state, it is probably missing in the server"
            }

            val targetFile = File(parPath + File.separator + filename)
            if (!client.isLegacy) {
                handleOrientation(rNode, targetFile.absolutePath)
            }

            fileService.registerLocalFile(state, rNode, type, targetFile)
            return filename to null
        } catch (e: java.lang.Exception) {
            Log.e(logTag, "could not get thumb for $state: ${e.message}")
            // TODO improve At this point, if we had an error, the target file is most probably corrupted or missing
            //   Problem: if we are offline we might reach this point and and remove the record too fast. 
            // e.printStackTrace()
            fileService.unregisterLocalFile(state, type)
            return null to "Get thumb for $state failed: ${e.message}"
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
    suspend fun uploadOne(stateID: StateID) = withContext(Dispatchers.IO) {
        val dao = getTransferDao(stateID)
        val uploadRecord = dao.getByState(stateID.id)
            ?: throw IllegalStateException("No transfer record found for $stateID, cannot upload")
        doUpload(dao, uploadRecord)
    }

    suspend fun uploadOne(accountId: StateID, transferId: Long) = withContext(Dispatchers.IO) {
        val dao = getTransferDao(accountId)
        val uploadRecord = dao.getById(transferId)
            ?: throw IllegalStateException("No transfer record found for $transferId in $accountId, cannot upload")
        doUpload(dao, uploadRecord)
    }

    private suspend fun doUpload(dao: TransferDao, transferRecord: RTransfer) =
        withContext(Dispatchers.IO) {
            val stateID = transferRecord.getStateID()
                ?: throw IllegalStateException("Cannot start upload with no defined StateID")
            try {
                // Mark the upload as started
                transferRecord.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
                // Also reset in case of restart, to be refined
                transferRecord.error = null
                transferRecord.status = AppNames.JOB_STATUS_PROCESSING
                dao.update(transferRecord)

                val srcPath =
                    fileService.getLocalPathFromState(stateID, AppNames.LOCAL_FILE_TYPE_FILE)
                val srcFile = File(srcPath)

                if (!accountService.getClient(stateID).isLegacy) {
                    doS3Upload(
                        context = CellsApp.instance.applicationContext,
                        stateID = stateID,
                        file = srcFile,
                        dao = dao,
                        transferRecord = transferRecord
                    )
                } else {
                    doP8Upload(
                        stateID = stateID,
                        srcFile = srcFile,
                        dao = dao,
                        transferRecord = transferRecord
                    )
                }
            } catch (e: Exception) {
                if (e is SDKException && e.code == ErrorCodes.cancelled) {
                    Log.e(logTag, "... Got cancelled, acknowledging message...")
                    // FIXME there is still some kind of routine leak here...
                    //   the cancel kind of hang out and prevents the new (2nd) upload to gracefully finish.
                    this.cancel(message = e.message ?: "Explicitly cancelled", cause = e)
                } else {
                    Log.e(logTag, "... Got an error but it is not a cancel: $e")
                    e.printStackTrace()
                    transferRecord.error = e.message
                    transferRecord.status = AppNames.JOB_STATUS_ERROR
                    transferRecord.doneTimestamp = currentTimestamp()
                }
            } finally {
                dao.update(transferRecord)
            }
        }

    private suspend fun doS3Upload(
        context: Context,
        stateID: StateID,
        file: File,
        dao: TransferDao,
        transferRecord: RTransfer
    ) =
        withContext(Dispatchers.IO) {
            Log.i(logTag, "TU upload for ${file.absolutePath} (size: ${file.length()}")

            val key = CellsS3Client.getCleanPath(stateID)
            Log.d(
                logTag, "... About to put\n " +
                        "- key: [$key] \n" +
                        "- file: ${file.absolutePath}"
            )

            val transferUtility = getTransferUtility(context, accountService, stateID)
                ?: throw SDKException("Could not get a transferUtility to upload to $stateID")
            Log.e(logTag, "... Got a transferUtility, uploading ${transferRecord.transferId}")
            val observer = transferUtility.upload(key, file)
            // TODO improve: we loose the listener if the app is restarted during the upload
            observer.setTransferListener(CellsTransferListener(this, dao, transferRecord))

            // Gets id of the transfer.
            // val id = observer.id
            // Pauses the transfer.
//            transferUtility.pause(id);
//            // Pause all the transfers.
//            transferUtility.pauseAllWithType(TransferType.ANY);
//            // Resumes the transfer.
//            transferUtility.resume(id);
//            // Resume all the transfers.
//            transferUtility.resumeAllWithType(TransferType.ANY);

//            // For canceling and deleting tasks:
//            // Cancels the transfer.
//            transferUtility.cancel(id);
//            // Cancel all the transfers.
//            transferUtility.cancelAllWithType(TransferType.ANY);
//            // Deletes the transfer.
//            transferUtility.cancel(id);
//            transferUtility.deleteTransferRecord(id);
        }


    private fun doP8Upload(
        stateID: StateID,
        srcFile: File,
        dao: TransferDao,
        transferRecord: RTransfer
    ) {
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
                transferRecord.mime, parentID.workspace, parentID.file, stateID.fileName,
                true
            ) { progressL ->

                byteWritten += progressL

                cancellationMsg = dao.hasBeenCancelled(transferRecord.transferId)?.let {
                    val msg = "Upload cancelled by ${it.owner}"
                    transferRecord.status = AppNames.JOB_STATUS_CANCELLED
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
                    transferRecord.status = AppNames.JOB_STATUS_PROCESSING
                    dao.update(transferRecord)
                    byteWritten = 0
                    lastUpdateTS = newTs
                }
                cancellationMsg
            }
            Log.i(logTag, "### Done and not cancelled")
            transferRecord.error = null
            transferRecord.doneTimestamp = currentTimestamp()
            transferRecord.status = AppNames.JOB_STATUS_DONE
            // Also send remaining bits to the progress bar
            transferRecord.progress += byteWritten
            Log.e(logTag, "... ${transferRecord.progress} / ${transferRecord.byteSize}")
        } finally {
            IoHelpers.closeQuietly(inputStream)
        }
    }

    /**
     * Register a new upload that has to be processed
     */
    suspend fun register(
        cr: ContentResolver,
        uri: Uri,
        parentID: StateID,
        parentJobID: Long = -1,
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
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
            return@withContext Pair(-1, "")
        }

        val filename = name!!

        // Mime Type
        val mime = cr.getType(uri) ?: SdkNames.NODE_MIME_DEFAULT
        Log.e(logTag, "Enqueuing upload for $filename, MIME: [$mime], size: $size")

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

        val rec = RTransfer.createNew(
            AppNames.TRANSFER_TYPE_UPLOAD,
            size,
            mime,
            parentJobId = parentJobID,
        )
        return@withContext Pair(nodeDB(parentID).transferDao().insert(rec), filename)
    }

    /**
     * Make a local copy of the file from the device to an in-app folder in order to
     * workaround some permission issues
     * TODO improve this
     */
    suspend fun launchCopy(
        cr: ContentResolver,
        uri: Uri,
        parentID: StateID,
        transferID: Long,
        filename: String,
    ): StateID? = withContext(Dispatchers.IO) {

        val dao = getTransferDao(parentID)
        val uploadRecord = dao.getById(transferID)
            ?: throw IllegalStateException("No transfer record found for $transferID, cannot upload")

        //   in Cells app storage
        val fs = fileService
        val targetStateID = createLocalState(parentID, filename)
        val localPath =
            fs.getLocalPathFromState(targetStateID, AppNames.LOCAL_FILE_TYPE_FILE)
        val localFile = File(localPath)
        localFile.parentFile!!.mkdirs()

        // TODO we can also add a progress at this point
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = cr.openInputStream(uri)
            outputStream = FileOutputStream(localFile)
            IoHelpers.pipeRead(inputStream, outputStream)
        } catch (ioe: IOException) {
            Log.e(logTag, "could not create local copy of $filename: ${ioe.message}")
            ioe.printStackTrace()
            return@withContext null
        } finally {
            IoHelpers.closeQuietly(inputStream)
            IoHelpers.closeQuietly(outputStream)
        }

        uploadRecord.encodedState = targetStateID.id
        uploadRecord.localPath = localPath
        nodeDB(parentID).transferDao().update(uploadRecord)
        return@withContext targetStateID
    }

    /**
     * Does all the dirty work to copy the file from the device to an in-app folder
     * and to register a new transfer record in the DB
     */
    @Deprecated("rather use register() then copy()")
    suspend fun copyAndRegister(
        cr: ContentResolver,
        uri: Uri,
        parentID: StateID
    ): StateID? = withContext(Dispatchers.IO) {
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
            return@withContext null
        }

        val filename = name!!

        // Mime Type
        val mime = cr.getType(uri) ?: SdkNames.NODE_MIME_DEFAULT
        Log.e(logTag, "Enqueuing upload for $filename, MIME: [$mime], size: $size")

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
        val localPath =
            fs.getLocalPathFromState(targetStateID, AppNames.LOCAL_FILE_TYPE_FILE)
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
            return@withContext null
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
        Log.e(logTag, "... caching done for $filename, MIME: [$mime], size: $size")
        return@withContext targetStateID
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
    @SuppressLint("SdCardPath")
    suspend fun createDummyTransfers(accountID: StateID) = withContext(Dispatchers.IO) {
        val dao = getTransferDao(accountID)
        var i = 0
        while (i < 10000) {

            if (dao.getTransferCount() < 20) {

                Log.e(logTag, "Creating job with id $i")
                val recInit = RTransfer.fromState(
                    accountID.withPath("/common-files/dummy/$i").id,
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
