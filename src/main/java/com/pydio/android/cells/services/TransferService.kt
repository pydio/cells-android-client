package com.pydio.android.cells.services

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTransferCancellation
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
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
    private val mimeMap = MimeTypeMap.getSingleton()

    private val transferServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + transferServiceJob)

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

    fun getLiveTransfer(stateId: StateID): LiveData<RTransfer?> {
        return getTransferDao(stateId).getLiveByState(stateId.id)
    }

    fun getLiveRecord(accountId: StateID, transferUid: Long): LiveData<RTransfer?> {
        return getTransferDao(accountId).getLiveById(transferUid)
    }

    fun getTransferById(accountId: StateID, transferId: Long): RTransfer? {
        return getTransferDao(accountId).getById(transferId)
    }

    suspend fun clearTerminated(stateId: StateID) = withContext(Dispatchers.IO) {
        nodeDB(stateId).transferDao().clearTerminatedTransfers()
    }

    suspend fun deleteRecord(stateId: StateID, transferId: Long) = withContext(Dispatchers.IO) {
        nodeDB(stateId).transferDao().deleteTransfer(transferId)
    }

    suspend fun cancelTransfer(stateId: StateID, transferId: Long) = withContext(Dispatchers.IO) {
        val dao = nodeDB(stateId).transferDao()
        dao.insert(RTransferCancellation.cancel(stateId.id, transferId))
    }

    /** DOWNLOADS **/
    suspend fun getOrDownloadFile(state: StateID, type: String): File? =
        withContext(Dispatchers.IO) {

            val nodeDB = treeNodeRepository.nodeDB(state)
            val rNode = nodeDB.treeNodeDao().getNode(state.id)
            if (rNode == null) {
                // No node found, aborting
                Log.e(logTag, "No node found for $state, aborting $type DL")
                return@withContext null
            }

            // Detect if file is there and still up to date
            // TODO handle case when we are not connected
            fileService.getLocalFile(state, rNode, type)?.let {
                return@withContext it
            }

            val node = FileNode()
            node.properties = rNode.properties
            node.meta = rNode.meta

            val parentFolder = fileService.dataParentPath(state.accountId, type)

            val filename = when (type) {
                AppNames.LOCAL_FILE_TYPE_THUMB,
                AppNames.LOCAL_FILE_TYPE_PREVIEW -> dlThumb(state, rNode, parentFolder, type)
                AppNames.LOCAL_FILE_TYPE_FILE -> {
                    val rec = RTransfer.fromState(
                        state.id,
                        AppNames.TRANSFER_TYPE_DOWNLOAD,
                        parentFolder + state.path,
                        rNode.size,
                        rNode.mime,
                    )
                    val recId = nodeDB.transferDao().insert(rec)
                    getOrDownloadFile(state, recId)
                }
                else -> null
            }
            return@withContext filename?.let { childFile(parentFolder, filename) }
        }

    private suspend fun dlThumb(
        state: StateID,
        rNode: RTreeNode,
        parPath: String,
        type: String
    ): String? =
        withContext(Dispatchers.IO) {
            val node = FileNode()
            node.properties = rNode.properties
            node.meta = rNode.meta
            try {
                val client = accountService.getClient(state)

                val dim = when (type) {
                    AppNames.LOCAL_FILE_TYPE_THUMB -> 300
                    else -> 1024 // AppNames.LOCAL_FILE_TYPE_PREVIEW
                }
                val filename = client.getThumbnail(state, node, File(parPath), dim)
                val targetFile = File(parPath + File.separator + filename)
                if (!client.isLegacy) {
                    handleOrientation(rNode, targetFile.absolutePath)
                }

                fileService.registerLocalFile(state, rNode, type, targetFile)
                return@withContext filename
            } catch (e: java.lang.Exception) {
                Log.e(logTag, "could not get thumb for $state: ${e.message}")
                // At this point, if we had an error, the target file is most probably corrupted or missing
                fileService.unregisterLocalFile(state, type)
                return@withContext null
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

    /**
     * Centralize client specific actions that should be done **before** launching
     * the real download.
     */
    suspend fun prepareDownload(state: StateID, type: String): Pair<Long?, String?> =
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
            )
            return@withContext Pair(getTransferDao(state).insert(rec), null)
        }

    /**
     * Performs the real download for the pre-registered transfer record and update
     * both the RTreeNode and RTransfer records depending on the output status.
     */
    suspend fun getOrDownloadFile(accountId: StateID, transferUid: Long): String? =
        withContext(Dispatchers.IO) {

            var errorMessage: String? = null

            val dao = getTransferDao(accountId)

            // Retrieve data and sanity check
            val rTransfer = dao.getById(transferUid) ?: run {
                val msg = "No record found for $transferUid, aborting file DL"
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

            var out: FileOutputStream? = null
            try {

                Log.d(logTag, "About to download file from $state")

                targetFile.parentFile!!.mkdirs()
                out = FileOutputStream(targetFile)

                // Mark the upload as started
                rTransfer.startTimestamp = Calendar.getInstance().timeInMillis / 1000L
                rTransfer.status = AppNames.TRANSFER_STATUS_PROCESSING
                dao.update(rTransfer)

                // Real transfer
                accountService.getClient(state)
                    .download(state.workspace, state.file, out) { progressL ->

                        val canceled = dao.hasBeenCancelled(rTransfer.transferId) != null
                        if (canceled) {
                            val msg = "Download cancelled by user"
                            rTransfer.status = AppNames.TRANSFER_STATUS_CANCELED
                            rTransfer.doneTimestamp = currentTimestamp()
                            rTransfer.error = msg
                            errorMessage = msg
                        }

                        rTransfer.progress = progressL
                        dao.update(rTransfer)
                        canceled
                    }

                // Mark the upload as done
                if (rTransfer.status == AppNames.TRANSFER_STATUS_PROCESSING) {
                    rTransfer.status = AppNames.TRANSFER_STATUS_DONE
                    rTransfer.doneTimestamp = currentTimestamp()
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
                nodeDB(state).treeNodeDao().update(rNode)

            } catch (se: SDKException) { // Could not retrieve file, failing silently for the end user
                errorMessage = "Could not download file for " + state + ": " + se.message
                rTransfer.status = AppNames.TRANSFER_STATUS_ERROR
            } catch (ioe: IOException) {
                // TODO Could not write the file in the local fs, we should notify the user
                rTransfer.status = AppNames.TRANSFER_STATUS_ERROR
                errorMessage =
                    "Could not write file for DL of $state to the local device: ${ioe.message}"
                ioe.printStackTrace()
            } finally {
                IoHelpers.closeQuietly(out)
            }
            if (Str.notEmpty(errorMessage)) {
                rTransfer.doneTimestamp = currentTimestamp()
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

    private fun createTargetFile(parentID: StateID, name: String): File {
        val targetStateID = createLocalState(parentID, name)
        val localPath =
            fileService.getLocalPathFromState(targetStateID, AppNames.LOCAL_FILE_TYPE_FILE)
        val tf = File(localPath)
        tf.parentFile!!.mkdirs()
        return tf
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
