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

    /**
     * Get the path to a local resource, if it exists:
     * typically, it returns null for the thumb file if it has not yet been downloaded
     * from the remote server and persisted
     */
    @Throws(java.lang.IllegalStateException::class)
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
        val fileRecord = dao.getFile(stateID.id, type) ?: return true
        return remote.lastModified <= fileRecord.remoteTS &&
                remote.eTag == fileRecord.etag
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

    private fun isFileInLineWithIndex(rTreeNode: RTreeNode, rFile: RLocalFile): Boolean {
        return rTreeNode.etag == rFile.etag && rTreeNode.remoteModificationTS == rFile.remoteTS
    }

//    fun getThumbPath(item: RTreeNode): String? {
//        return null
//
////        return if (Str.empty(item.thumbFilename)) {
////            null
////        } else {
////            "${
////                dataParentPath(
////                    item.getStateID().accountId,
////                    AppNames.LOCAL_FILE_TYPE_THUMB
////                )
////            }${sep}${item.thumbFilename}"
////        }
//    }

//    fun getOfflineThumbPath(item: RLiveOfflineRoot): String? {
//        return null
////        return if (Str.empty(item.thumbFilename)) {
////            null
////        } else {
////            "${
////                dataParentPath(
////                    item.getStateID().accountId,
////                    AppNames.LOCAL_FILE_TYPE_THUMB
////                )
////            }${sep}${item.thumbFilename}"
////        }
//    }

//    fun getAccountBasePath(stateID: StateID, type: String): String {
//        val dirName = treeNodeRepository.sessions[stateID.accountId]?.dirName
//            ?: throw IllegalStateException("No record found for $stateID")
//        val middle = sep + dirName
//        return when (type) {
//            AppNames.LOCAL_DIR_TYPE_CACHE ->
//                appCacheDir + middle
//            AppNames.LOCAL_DIR_TYPE_FILE ->
//                appFilesDir + middle
//            else -> throw IllegalStateException("Unknown base folder type: $type")
//        }
//    }

    fun createImageFile(stateID: StateID): File {
        val timestamp = getCurrentDateTime().asFormattedString("yyMMdd_HHmmss")
        val imgPath = dataParentPath(stateID.account(), AppNames.LOCAL_FILE_TYPE_TRANSFER)
        // Superstition? the tree structure is created at account registration. Is it not enough?
        File(imgPath).mkdirs()
        return File("${imgPath}${sep}IMG_${timestamp}.jpg")

        // Would be safer but with an ugly name :(
        //        return File.createTempFile(
//            "IMG_${timestamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        )
    }

    fun cleanFileCacheFor(stateID: StateID, dirName: String) = serviceScope.launch {

        val cache = File(CellsApp.instance.cacheDir.absolutePath + sep + dirName)
        if (cache.exists()) {
            cache.deleteRecursively()
        }

        // FIXME we should do this more finely:
        // -> walk the tree
        // -> check if the current path fits with an offline root

//        val tmpCache = File(staticDataParentPath(dirName, AppNames.LOCAL_FILE_TYPE_CACHE))
//        if (tmpCache.exists()) {
//            tmpCache.deleteRecursively()
//        }

    }

    fun cleanAllLocalFiles(stateID: StateID, dirName: String) = serviceScope.launch {
        cleanFileCacheFor(stateID, dirName)

        val files = File(CellsApp.instance.filesDir.absolutePath + sep + dirName)
        if (files.exists()) {
            files.deleteRecursively()
        }
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
