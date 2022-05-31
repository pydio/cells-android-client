package com.pydio.android.cells.services

import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLocalFile
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.utils.asFormattedString
import com.pydio.android.cells.utils.getCurrentDateTime
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/** Centralizes management of local files and where to store/find them. */
class FileService(private val treeNodeRepository: TreeNodeRepository) {

    private val tag = FileService::class.simpleName

    private val fileServiceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + fileServiceJob)
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

    fun dataParentPath(accountId: StateID, type: String): String {
        val dirName = treeNodeRepository.sessions[accountId.accountId]?.dirName
            ?: throw IllegalStateException("No record found for $accountId")
        return staticDataParentPath(dirName, type)
    }

    /**
     * Get the path to a local resource, if it exists:
     * typically, it returns null for the thumb file if it has not yet been downloaded
     * from the remote server and persisted
     */
    fun getLocalPath(item: RTreeNode, type: String): String {
        val stat = StateID.fromId(item.encodedState)
        return getLocalPathFromState(stat, type)
    }

    fun getLocalPathFromState(stat: StateID, type: String): String {
        return when (type) {
            AppNames.LOCAL_FILE_TYPE_FILE,
            AppNames.LOCAL_FILE_TYPE_TRANSFER
            -> "${dataParentPath(stat.account(), type)}${stat.path}"
            AppNames.LOCAL_FILE_TYPE_THUMB,
            AppNames.LOCAL_FILE_TYPE_PREVIEW
            -> "${dataParentPath(stat.account(), type)}${stat.file}"
            else -> throw IllegalStateException("Cannot create $type path for $stat")
        }
    }

    /* LOCAL FILES (for offline and cache) */
//    suspend fun registerLocalFile(
    fun registerLocalFile(
        stateID: StateID,
        rTreeNode: RTreeNode,
        type: String,
        file: File
    ) {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        val rLocalFile =
            RLocalFile.fromFile(stateID, type, file, rTreeNode.etag, rTreeNode.remoteModificationTS)
        dao.insert(rLocalFile)
    }

    fun needsUpdate(stateID: StateID, remote: FileNode, type: String): Boolean {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        val fileRecord = dao.getFile(stateID.id, type) ?: let {
            Log.d(tag, "$type not found for ${stateID.file}, downloading")
            return true
        }
        val hasChanged = !(remote.lastModified <= fileRecord.remoteTS &&
                remote.eTag == fileRecord.etag)
        if (hasChanged) {
            Log.d(
                tag, "$type for ${stateID.file} needs update:\n" +
                        "  ${remote.lastModified} - ${fileRecord.remoteTS} \n" +
                        "  ${remote.eTag} - ${fileRecord.etag}"
            )
        }
        return hasChanged
    }

    fun unregisterLocalFile(stateID: StateID, type: String) {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        dao.delete(stateID.id, type)
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

    /** This also checks that the file is in line with the index */
    fun getLocalFile(stateID: StateID, rTreeNode: RTreeNode, type: String): File? {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()

        val rFile = dao.getFile(stateID.id, type) ?: let {
            Log.e(tag, "no record for [$type]: $stateID")
            return null
        }
        val parPath = dataParentPath(stateID.account(), type)
        val file = File(parPath + File.separator + rFile.file)
        if (!file.exists()) {
            Log.e(tag, "could not find file at ${file.absolutePath}")
            return null
        }
        if (!isFileInLineWithIndex(rTreeNode, rFile)) {
            Log.e(tag, "remote file has changed")
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

        // First clean the files that are not in an offline root sub-tree
        // We retrieve defined offline roots
        val offlineDao = treeNodeRepository.nodeDB(accountID).offlineRootDao()
        val offlinePaths = offlineDao.getAll().map { it.encodedState }
        // We then iterate on all files
        val filesDao = treeNodeRepository.nodeDB(accountID).localFileDao()
        for (record in filesDao.getFilesUnder(accountID.id)) {
            if (!isInOfflineTree(offlinePaths, record.encodedState)) {
                filesDao.delete(record.encodedState, record.type)
            }
        }

        // We then also clean transfer temporary files
        val transferDir = File(dataParentPath(accountID, AppNames.LOCAL_FILE_TYPE_TRANSFER))
        if (transferDir.exists()) {
            transferDir.deleteRecursively()
        }
    }

    /* Violently remove all local files and also empty the local_files table */
    suspend fun cleanAllLocalFiles(accountID: StateID, dirName: String) {

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
            Log.w(tag, m)
            return null
        }
        return file
    }

}
