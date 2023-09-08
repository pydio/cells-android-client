package com.pydio.android.cells.services

import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLocalFile
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.utils.asFormattedString
import com.pydio.android.cells.utils.computeFileMd5
import com.pydio.android.cells.utils.getCurrentDateTime
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import java.io.File

/** Centralizes management of local files and where to store/find them. */
class FileService(
    coroutineService: CoroutineService,
    private val treeNodeRepository: TreeNodeRepository
) {

    private val logTag = "FileService"

    private val serviceScope = coroutineService.cellsIoScope
    private val sep: String = File.separator

    private val appCacheDir = CellsApp.instance.cacheDir.absolutePath
    private val appFilesDir = CellsApp.instance.filesDir.absolutePath

    fun prepareTree(stateID: StateID) = serviceScope.launch {
        val account = stateID.account()
        File(dataParentPath(account, AppNames.LOCAL_FILE_TYPE_THUMB)).mkdirs()
        File(dataParentPath(account, AppNames.LOCAL_FILE_TYPE_PREVIEW)).mkdirs()
        File(dataParentPath(account, AppNames.LOCAL_FILE_TYPE_FILE)).mkdirs()
        File(dataParentPath(account, AppNames.LOCAL_FILE_TYPE_TRANSFER)).mkdirs()
    }

    fun dataParentPath(accountID: StateID, type: String): String {
        val dirName = treeNodeRepository.sessions[accountID.accountId]?.dirName
            ?: throw IllegalStateException("No record found for [$accountID]")
        return staticDataParentPath(dirName, type)
    }

    /**
     * Get the path to a local resource, if it exists:
     * typically, it returns null for the thumb file if it has not yet been downloaded
     * from the remote server and persisted
     */
    fun getLocalPath(item: RTreeNode, type: String): String {
        return getLocalPathFromState(item.getStateID(), type)
    }

    fun getLocalPathFromState(state: StateID, type: String): String {
        return when (type) {
            AppNames.LOCAL_FILE_TYPE_FILE,
            AppNames.LOCAL_FILE_TYPE_TRANSFER
            -> "${dataParentPath(state.account(), type)}${state.path}"

            AppNames.LOCAL_FILE_TYPE_THUMB,
            AppNames.LOCAL_FILE_TYPE_PREVIEW
            -> "${dataParentPath(state.account(), type)}${state.file}"

            else -> throw IllegalStateException("Cannot create $type path for $state")
        }
    }

    /* LOCAL FILES (for offline and cache) */
    fun registerLocalFile(stateID: StateID, rTreeNode: RTreeNode, type: String, file: File) {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        val rLocalFile =
            RLocalFile.fromFile(stateID, type, file, rTreeNode.etag, rTreeNode.remoteModificationTS)
        dao.insert(rLocalFile)
    }

    fun registerLocalFile(rTransfer: RTransfer) {
        val tid = rTransfer.transferId
        val stateID = rTransfer.getStateID() ?: run {
            Log.e(logTag, "Transfer #$tid has no StateID, could not register, aborting")
            return
        }
        val localPath = rTransfer.localPath ?: run {
            Log.e(logTag, "Transfer #$tid has no localPath, could not register, aborting")
            return
        }
        val localFile = File(localPath)
        if (!localFile.exists()) {
            Log.e(
                logTag,
                "Could not find file at $localFile for $stateID. Could not register, aborting"
            )
            return
        }
        // val type = rTransfer.type // <- This is not OK, transfer type is download or upload
        // TODO improve when also adding the preview and thumb to the transfer mechanism
        val type = AppNames.LOCAL_FILE_TYPE_FILE
        val ndb = treeNodeRepository.nodeDB(stateID.account())
        val treeNode = ndb.treeNodeDao().getNode(stateID.id) ?: run {
            Log.e(logTag, "Transfer #$tid points toward an un-existing node at $stateID, aborting")
            return
        }
        val rLocalFile = RLocalFile.fromFile(
            stateID, type, localFile,
            treeNode.etag, treeNode.remoteModificationTS
        )
        Log.i(logTag, "... #$tid - After transfer, registering local file: $rLocalFile")
        ndb.localFileDao().insert(rLocalFile)
    }

    fun needsUpdate(stateID: StateID, remote: FileNode, type: String): Boolean {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        val fileRecord = dao.getFile(stateID.id, type) ?: let {
            Log.d(logTag, "$type not found for ${stateID.file}, downloading")
            return true
        }
        val hasChanged = !(remote.lastModified <= fileRecord.remoteTS &&
                remote.eTag == fileRecord.etag)
        if (hasChanged) {
            Log.d(
                logTag, "$type for ${stateID.file} needs update:\n" +
                        "  ${remote.lastModified} - ${fileRecord.remoteTS} \n" +
                        "  ${remote.eTag} - ${fileRecord.etag}"
            )
        }
        return hasChanged
    }

    fun isLocalFileUpToDate(updatedNode: RTreeNode, localFile: RLocalFile): Boolean {

        // First simple checks
        var upToDate = updatedNode.remoteModificationTS <= localFile.remoteTS &&
                updatedNode.etag == localFile.etag
        if (!upToDate) {
            return false
        }
        // Also double check the file is here
        val parPath = dataParentPath(updatedNode.getAccountID(), AppNames.LOCAL_FILE_TYPE_FILE)
        val lf = File(parPath + File.separator + localFile.file)
        upToDate = lf.exists()
        if (!upToDate) {
            return false
        }

        if (localFile.etag == null) { // We have a P8 distant server, no more checks
            return true
        }

        // Finally recompute local file md5 to insure it corresponds with the expected value (corrupted file)
        val computedMd5 = computeFileMd5(lf)
        if (localFile.etag != computedMd5) {
            // This should never happen, we expect that the md5 check is done at DL time.
            Log.w(logTag, "MD5 signatures do not match when trying to DL local file to device")
            Log.d(logTag, "Expected: [${localFile.etag}], computed: [$computedMd5]")
        }
        return localFile.etag == computedMd5
    }

    /** This also checks that the file is in line with the index */
    fun getLocalFile(stateID: StateID, rTreeNode: RTreeNode, type: String): File? {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()

        val rFile = dao.getFile(stateID.id, type) ?: let {
            Log.d(logTag, "No record for $type file: [ $stateID ]")
            return null
        }
        val parPath = dataParentPath(stateID.account(), type)
        val file = File(parPath + File.separator + rFile.file)
        if (!file.exists()) {
            Log.d(logTag, "Could not find file at ${file.absolutePath}")
            return null
        }
        if (!isFileInLineWithIndex(rTreeNode, rFile)) {
            Log.d(logTag, "Remote file has changed for [$stateID]")
            return null
        }
        return file
    }

    fun createImageFile(stateID: StateID): File {
        val timestamp = getCurrentDateTime().asFormattedString("yyMMdd_HHmmss")
        val imgPath = dataParentPath(stateID.account(), AppNames.LOCAL_FILE_TYPE_TRANSFER)
        // Superstition? the tree structure is created at account registration. Is it not enough?
        File(imgPath).mkdirs()
        return File("${imgPath}${sep}IMG_${timestamp}.jpg")
    }

    fun cleanFileCacheFor(accountID: StateID) = serviceScope.launch {
        try {
            // Defined offline roots that must be left intact
            val offlineDao = treeNodeRepository.nodeDB(accountID).offlineRootDao()
            val offlinePaths = offlineDao.getAllActive().map { it.encodedState }

            // Iterate on all files both in cache/<id>/{previews,thumbs} and in files/<id>/local
            // and delete the ones that are not in offline roots
            val filesDao = treeNodeRepository.nodeDB(accountID).localFileDao()
            for (record in filesDao.getFilesUnder(accountID.id)) {
                if (!isInOfflineTree(offlinePaths, record.encodedState)) {
                    filesDao.delete(record.encodedState, record.type)
                }
            }

            // Also violently wipe transfer temporary files
            val transferDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_TRANSFER))
            if (transferDir.exists()) {
                transferDir.deleteRecursively()
            }
        } catch (e: Exception) {
            // TODO better error handling
            Log.e(logTag, "Could not clean cache for $accountID: ${e.message}")
            e.printStackTrace()
        }
    }

    /* Violently remove all local files and also empty the local_files table */
    fun cleanAllLocalFiles(accountID: StateID) {

        // Recursively delete local folders
        var currDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_THUMB))
        if (currDir.exists()) {
            currDir.deleteRecursively()
        }
        currDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_PREVIEW))
        if (currDir.exists()) {
            currDir.deleteRecursively()
        }
        currDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_FILE))
        if (currDir.exists()) {
            currDir.deleteRecursively()
        }
        currDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_TRANSFER))
        if (currDir.exists()) {
            currDir.deleteRecursively()
        }

        // Also empty the local_files table
        val localFileDao = treeNodeRepository.nodeDB(accountID).localFileDao()
        localFileDao.deleteUnder(accountID.id)
    }

    fun unregisterLocalFile(stateID: StateID, type: String) {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        dao.getFile(stateID.id, type)?.let {
            getFileFromRecord(it)?.delete()
            dao.delete(stateID.id, type)
        }
    }

    fun deleteCachedFilesFor(rTreeNode: RTreeNode) {
        val dao = treeNodeRepository.nodeDB(rTreeNode.getAccountID()).localFileDao()
        val fileRecords = dao.getFiles(rTreeNode.encodedState)
        for (record in fileRecords) {
            getFileFromRecord(record)?.delete()
        }
        dao.delete(rTreeNode.encodedState)
    }

    fun deleteCachedFileRecursively(folderId: StateID) {
        val dao = treeNodeRepository.nodeDB(folderId.account()).localFileDao()
        val fileRecords = dao.getFilesUnder(folderId.id)
        for (record in fileRecords) {
            getFileFromRecord(record)?.delete()
        }
        dao.deleteUnder(folderId.id)
    }

    // Local helpers
    private fun isInOfflineTree(rootPaths: List<String>, currentPath: String): Boolean {
        return rootPaths.any { currentPath.startsWith(it) }
    }

    private fun staticDataParentPath(currDirName: String, type: String): String {
        val middle = sep + currDirName + sep
        return when (type) {
            AppNames.LOCAL_FILE_TYPE_THUMB ->
                appCacheDir + middle + AppNames.THUMB_PARENT_DIR

            AppNames.LOCAL_FILE_TYPE_PREVIEW ->
                appCacheDir + middle + AppNames.PREVIEW_PARENT_DIR
            // TODO we cannot put this in the default cache folder for now:
            //   we do not know how to configure the file provider to allow access to this location
            //   for external viewing
            AppNames.LOCAL_FILE_TYPE_FILE ->
                appFilesDir + middle + AppNames.LOCAL_FILE_PARENT_DIR
            // Same with the transfer folder, see:
            // https://developer.android.com/training/data-storage/shared/media#open-file-descriptor
            AppNames.LOCAL_FILE_TYPE_TRANSFER ->
                appFilesDir + middle + AppNames.TRANSFER_PARENT_DIR

            else -> throw IllegalStateException("Unknown file type: $type")
        }
    }

    private fun isFileInLineWithIndex(rTreeNode: RTreeNode, rFile: RLocalFile): Boolean {
        return rTreeNode.etag == rFile.etag && rTreeNode.remoteModificationTS == rFile.remoteTS
    }

    /** We also check if the file exists and return null otherwise */
    private fun getFileFromRecord(record: RLocalFile): File? {
        val parPath = dataParentPath(record.getAccountID(), record.type)
        val file = File(parPath + File.separator + record.file)
        if (!file.exists()) {
            val m = "${record.getStateID()}: Missing ${record.type} record at ${file.absolutePath}"
            Log.w(logTag, m)
            return null
        }
        return file
    }
}
