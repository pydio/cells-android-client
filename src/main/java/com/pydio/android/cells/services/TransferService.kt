package com.pydio.android.cells.services

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.sqlite.db.SimpleSQLiteQuery
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.utils.childFile
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    androidApplicationContext: Context,
    coroutineService: CoroutineService,
    private val prefs: PreferencesService,
    private val networkService: NetworkService,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
    private val nodeService: NodeService,
    private val fileService: FileService,
) {

    private val logTag = "TransferService"

    private val serviceScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    // TODO: rather use dependency injection
    private val s3TransferService =
        S3TransferService(
            androidApplicationContext,
            coroutineService,
            accountService,
            treeNodeRepository
        )
    private val p8TransferService =
        P8TransferService(coroutineService, accountService, treeNodeRepository, fileService)

    companion object {
        // Hard-coded constants to ease implementation in a first pass. TODO: improve
        const val thumbDim = 300
        const val previewDim = 1024

        // The 2 below value are rough average for thumb and preview downloads
        const val thumbSize: Long = 20 * 1024
        const val previewSize: Long = 200 * 1024
    }

    fun liveTransfer(accountID: StateID, transferID: Long): Flow<RTransfer?> {
        return nodeDB(accountID).transferDao().getLiveById(transferID)
    }

    fun queryTransfersExplicitFilter(
        stateID: StateID,
        filterByStatus: String,
        encodedOrder: String,
    ): Flow<List<RTransfer>> {
        val (sortByCol, sortByOrder) = parseOrder(encodedOrder, ListType.TRANSFER)
        val lsQuery = if (filterByStatus == JobStatus.NO_FILTER.id) {
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

    /** Dynamic list of all transfers for a given JobID, mainly used in Shared activity */
    fun getChildTransfersRecords(accountID: StateID, jobID: Long): Flow<List<RTransfer>> {
        // tweak to insure we return no jobs when no job ID has been explicitly set
        val id = if (jobID < 1) -1 else jobID
        return nodeDB(accountID).transferDao().getByJobId(id)
    }


    suspend fun getRecord(accountID: StateID, transferID: Long): RTransfer? =
        withContext(ioDispatcher) {
            return@withContext getTransferDao(accountID).getById(transferID)
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

    suspend fun clearTerminated(stateID: StateID) = withContext(ioDispatcher) {
        val dao = nodeDB(stateID).transferDao()
        // val before = dao.getTransferCount()

        dao.clearTerminatedTransfers()
        // We also remove unterminated jobs that have not been updated since more than 10 minutes
        dao.clearStaleTransfers(currentTimestamp() - 600)
        // val after = dao.getTransferCount()
        // Log.e(logTag, "After transfer clean: $after (B4: $before)")
    }

    suspend fun forgetTransfer(
        stateID: StateID,
        transferID: Long,
        isRemoteLegacy: Boolean = false
    ) {
        if (isRemoteLegacy) {
            p8TransferService.forgetTransfer(stateID, transferID)
        } else {
            s3TransferService.forgetTransfer(stateID, transferID)
        }
    }

    suspend fun cancelTransfer(
        stateId: StateID,
        transferID: Long,
        owner: String,
        isRemoteLegacy: Boolean = false
    ) {
        if (isRemoteLegacy) {
            p8TransferService.cancelTransfer(stateId, transferID, owner)
        } else {
            s3TransferService.cancelTransfer(stateId, transferID, owner)
        }
    }

    suspend fun pauseTransfer(
        stateId: StateID,
        transferID: Long,
        owner: String,
        isRemoteLegacy: Boolean = false
    ) {
        if (isRemoteLegacy) {
            p8TransferService.pauseTransfer(stateId, transferID, owner)
        } else {
            s3TransferService.pauseTransfer(stateId, transferID, owner)
        }
    }

    suspend fun resumeTransfer(
        stateId: StateID,
        transferID: Long,
        isRemoteLegacy: Boolean = false
    ) {
        if (isRemoteLegacy) {
            p8TransferService.resumeTransfer(stateId, transferID)
        } else {
            s3TransferService.resumeTransfer(stateId, transferID)
        }
    }

    /** DOWNLOADS **/
    suspend fun getImageForDisplay(
        stateID: StateID,
        type: String,
        parentJob: RJob?
    ): File = withContext(ioDispatcher) {
        val accountID = stateID.account()
        val nodeDB = treeNodeRepository.nodeDB(accountID)
        val rNode = nodeDB.treeNodeDao().getNode(stateID.id) ?: run {
            // No node found, aborting
            val errMsg = "No node found for $stateID, aborting $type DL"
            throw SDKException(ErrorCodes.no_local_node, errMsg)
        }

        // First try to retrieve local file
        fileService.getLocalFile(stateID, rNode, type)?.let {
            return@withContext it
        }

        // Otherwise, try to download if current network type and user preferences allow it
        val currSettings = prefs.fetchPreferences()
        when (networkService.fetchNetworkStatus()) {
            is NetworkStatus.Unmetered
            -> return@withContext downloadFile(stateID, rNode, type, parentJob, null)

            is NetworkStatus.Metered -> {
                if (!currSettings.meteredNetwork.applyLimits || currSettings.meteredNetwork.dlThumbs) {
                    return@withContext downloadFile(stateID, rNode, type, parentJob, null)
                } else {
                    throw SDKException(
                        ErrorCodes.con_failed,
                        "Cannot download preview images on metered network"
                    )
                }
            }

            else -> throw SDKException(
                ErrorCodes.con_failed,
                "No network connection: cannot download preview image"
            )
        }
    }

    @Throws(SDKException::class)
    suspend fun getFileForDiff(
        stateID: StateID,
        type: String,
        parentJob: RJob?,
        progressChannel: Channel<Long>?
    ): File = withContext(ioDispatcher) {
        val account = stateID.account()
        val nodeDB = treeNodeRepository.nodeDB(account)
        val rNode = nodeDB.treeNodeDao().getNode(stateID.id) ?: run {
            val errMsg = "No node found for $stateID, aborting $type DL"
            throw SDKException(ErrorCodes.no_local_node, errMsg)
        }
        downloadFile(stateID, rNode, type, parentJob, progressChannel)
    }

    @Suppress("BlockingMethodInNonBlockingContext") // injected dispatcher is not recognised by the linter
    @Throws(SDKException::class)
    suspend fun saveToSharedStorage(stateID: StateID, uri: Uri) = withContext(ioDispatcher) {
        val rTreeNode = nodeDB(stateID).treeNodeDao().getNode(stateID.id)
            ?: return@withContext
        var localFile = nodeService.getLocalFile(rTreeNode, AppNames.LOCAL_FILE_TYPE_FILE)
        val resolver = CellsApp.instance.contentResolver
        try {
            if (!localFile.exists() || (networkService.isConnected() && nodeService.isCachedVersionUpToDate(
                    rTreeNode
                ) == false)
            ) {
                val jobId = prepareDownload(stateID, AppNames.LOCAL_FILE_TYPE_FILE)
                runDownloadAndWait(stateID, jobId)
                localFile = nodeService.getLocalFile(rTreeNode, AppNames.LOCAL_FILE_TYPE_FILE)
            }

            // TODO if the user chooses a name that already exists,
            //   the downloaded doc might end up being corrupted and un-readable
            if (localFile.exists()) {
                var out: OutputStream? = null
                var input: InputStream? = null
                try {
                    out = resolver.openOutputStream(uri)
                    input = FileInputStream(localFile)
                    IoHelpers.pipeRead(input, out)
                } finally {
                    IoHelpers.closeQuietly(input)
                    IoHelpers.closeQuietly(out)
                }
            }
            Log.i(logTag, "... File has been copied to ${uri.path}")
        } catch (ioe: IOException) {
            throw SDKException(ErrorCodes.local_io_error, "cannot write at ${uri.path}", ioe)
        } catch (se: SDKException) {
            throw SDKException(
                ErrorCodes.internal_error,
                "could not perform DL for " + stateID.id,
                se
            )
        }
    }

    /**
     * Launch the effective download. No check is done anymore: we assume the caller has
     * done them before calling this method.
     */
    @Throws(SDKException::class)
    private suspend fun downloadFile(
        stateID: StateID,
        rNode: RTreeNode,
        type: String,
        parentJob: RJob?,
        parentJobProgress: Channel<Long>?
    ): File {
        val parentFolder = fileService.dataParentPath(stateID.account(), type)
        val filename = when (type) {
            AppNames.LOCAL_FILE_TYPE_THUMB,
            AppNames.LOCAL_FILE_TYPE_PREVIEW -> {
                val name = dlThumb(stateID, rNode, parentFolder, type)
                parentJobProgress?.send(
                    if (type == AppNames.LOCAL_FILE_TYPE_THUMB) thumbSize else previewSize
                )
                name
            }

            AppNames.LOCAL_FILE_TYPE_FILE -> {
                val jobId = prepareDownload(stateID, type, parentJob)
                runDownloadTransfer(stateID, jobId, parentJobProgress)
                // filename is...  (Note that we remove the leading slash to comply with childFile method signature, see just below)
                stateID.path.substring(1)
            }

            else -> throw SDKException(ErrorCodes.internal_error, "Unknown file type: $type")
        }
        return childFile(parentFolder, filename)
    }

    /**
     * Centralize client specific actions that should be done **before** launching
     * the real download.
     */
    @Throws(SDKException::class)
    suspend fun prepareDownload(
        stateID: StateID,
        type: String,
        parentJob: RJob? = null
    ): Long = withContext(ioDispatcher) {

        val rNode = nodeService.getNode(stateID)
        if (rNode == null) {
            val errorMessage = "No node found for $stateID, aborting file DL"
            throw SDKException(ErrorCodes.no_local_node, errorMessage)
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

        Log.d(logTag, "Prepared transfer #$transferID for $stateID")
        return@withContext transferID
    }

    /**
     * Performs the real download for the pre-registered transfer record and
     * wait until it is finished or we reach a 300 seconds timeout.
     */
    @Throws(SDKException::class)
    suspend fun runDownloadAndWait(
        accountID: StateID,
        transferID: Long,
        parentJobProgress: Channel<Long>? = null
    ) = withContext(ioDispatcher) {

        runDownloadTransfer(accountID, transferID, parentJobProgress)
        val dao = getTransferDao(accountID)
        val limit = currentTimestamp() + 300
        loop@ while (currentTimestamp() < limit) {
            val currRecord = dao.getById(transferID)
            if (currRecord != null && currRecord.doneTimestamp > 0) {
                break@loop
            }
            delay(2000)
        }
        if (currentTimestamp() > limit) {
            Log.e(logTag, "########################################")
            Log.e(logTag, "########################################")
            val msg = "## 5mn timeout reached while waiting for transfer #$transferID to complete"
            Thread.dumpStack()
            Log.e(logTag, "$msg, see stack above.")
            throw SDKException(ErrorCodes.timeout, msg)
        }
        Log.e(logTag, "## After wait")
    }

    /**
     * Performs the real download for the pre-registered transfer record and update
     * both the RTreeNode and RTransfer records depending on the output status.
     */
    @Throws(SDKException::class)
    suspend fun runDownloadTransfer(
        accountID: StateID,
        transferID: Long,
        parentJobProgress: Channel<Long>? = null
    ) = withContext(ioDispatcher) {

        val dao = getTransferDao(accountID)

        // Retrieve data and sanity check
        val rTransfer = dao.getById(transferID) ?: run {
            val msg = "No record found for $transferID, aborting file DL"
            throw SDKException(ErrorCodes.no_local_node, msg)
        }

        val localPath = rTransfer.localPath ?: run {
            val msg = "No local path is defined for $transferID, aborting file DL"
            throw SDKException(ErrorCodes.internal_error, msg)
        }

        val state = StateID.fromId(rTransfer.encodedState)
        val rNode = nodeService.getNode(state)
        if (rNode == null) {
            val errMsg = "No node found for $state, aborting file DL"
            throw SDKException(ErrorCodes.no_local_node, errMsg)
        }

        // Prepare target file
        val targetFile = File(localPath)
        targetFile.parentFile?.mkdirs()
        if (!accountService.getClient(state).isLegacy) {
            s3TransferService.doDownload(
                stateID = state,
                targetFile = targetFile,
                dao = dao,
                transferRecord = rTransfer,
                parentJobProgress = parentJobProgress,
            )
        } else {
            p8TransferService.doDownload(
                stateID = state,
                targetFile = targetFile,
                parentJobProgress = parentJobProgress,
                dao = dao,
                rTreeNode = rNode,
                rTransfer = rTransfer,
            )
        }
    }

    private suspend fun dlThumb(
        state: StateID,
        rNode: RTreeNode,
        parPath: String,
        type: String
    ): String {
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
                throw SDKException(
                    ErrorCodes.not_found,
                    "Could not get thumb for $state, it is probably missing in the server"
                )
            }

            val targetFile = File(parPath + File.separator + filename)
            if (!client.isLegacy) {
                handleOrientation(rNode, targetFile.absolutePath)
            }

            fileService.registerLocalFile(state, rNode, type, targetFile)
            return filename
        } catch (e: Exception) {
            Log.e(logTag, "could not get thumb for $state: ${e.message}")
            // TODO improve At this point, if we had an error, the target file is most probably corrupted or missing
            //   Problem: if we are offline we might reach this point and and remove the record too fast. 
            // e.printStackTrace()
            fileService.unregisterLocalFile(state, type)
            throw SDKException(ErrorCodes.not_found, "Get thumb for $state failed", e)
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
        // EXIF data must be manually retrieved from main image and applied
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
    suspend fun uploadOne(stateID: StateID) = withContext(ioDispatcher) {
        val dao = getTransferDao(stateID)
        val uploadRecord = dao.getByState(stateID.id)
            ?: throw IllegalStateException("No transfer record found for $stateID, cannot upload")
        doUpload(dao, uploadRecord)
    }

    suspend fun uploadOne(accountId: StateID, transferId: Long) = withContext(ioDispatcher) {
        val dao = getTransferDao(accountId)
        val uploadRecord = dao.getById(transferId)
            ?: throw IllegalStateException("No transfer record found for $transferId in $accountId, cannot upload")
        doUpload(dao, uploadRecord)
    }

    /**
     * Register a new upload that has to be processed
     */
    suspend fun register(
        cr: ContentResolver,
        uri: Uri,
        parentID: StateID,
        parentJobID: Long = -1,
    ): Pair<Long, String> = withContext(ioDispatcher) {
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
    ): StateID? = withContext(ioDispatcher) {

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
            @Suppress("BlockingMethodInNonBlockingContext")
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

    private suspend fun doUpload(dao: TransferDao, transferRecord: RTransfer) =
        withContext(ioDispatcher) {
            val stateID = transferRecord.getStateID()
                ?: throw IllegalStateException("Cannot start upload with no defined StateID")
            try {
                // Mark the upload as started
                transferRecord.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
                // Also reset in case of restart, to be refined
                transferRecord.error = null
                transferRecord.status = JobStatus.PROCESSING.id
                dao.update(transferRecord)

                val srcPath =
                    fileService.getLocalPathFromState(stateID, AppNames.LOCAL_FILE_TYPE_FILE)
                val srcFile = File(srcPath)

                if (!accountService.getClient(stateID).isLegacy) {
                    s3TransferService.doUpload(
                        stateID = stateID,
                        file = srcFile,
                        dao = dao,
                        transferRecord = transferRecord
                    )
                } else {
                    p8TransferService.doUpload(
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
                    transferRecord.status = JobStatus.ERROR.id
                    transferRecord.doneTimestamp = currentTimestamp()
                }
            } finally {
                dao.update(transferRecord)
            }
        }


    /* HELPERS */

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

    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }

    private fun getTransferDao(accountId: StateID): TransferDao {
        return nodeDB(accountId).transferDao()
    }

    /**
     * Debug method to easily debug transfers live Data issues
     */
    @SuppressLint("SdCardPath")
    suspend fun createDummyTransfers(accountID: StateID) = withContext(ioDispatcher) {
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
                rec.status = JobStatus.PROCESSING.id
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
                rec.status = JobStatus.DONE.id
                rec.doneTimestamp = rec.updateTimestamp
                dao.update(rec)
            }
            delay(2000)
            i++
        }
    }
}
