package com.pydio.android.cells.services

import com.pydio.android.cells.AppNames
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.nodes.RLocalFile
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.utils.asFormattedString
import com.pydio.android.cells.utils.getCurrentDateTime
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
        File(dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_THUMB)).mkdirs()
        File(dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_PREVIEW)).mkdirs()
        File(dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_FILE)).mkdirs()
        File(dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_TRANSFER)).mkdirs()
    }

//    fun dataParentFolder(stateID: StateID, type: String): File {
//        return File(dataParentPath(stateID.accountId, type))
//    }

    fun dataParentPath(accountId: String, type: String): String {
        val dirName = treeNodeRepository.sessions[accountId]?.dirName
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
     * and persisted from the remote server
     * */
    @Throws(java.lang.IllegalStateException::class)
    fun getLocalPath(item: RTreeNode, type: String): String {
        val stat = StateID.fromId(item.encodedState)
        return getLocalPathFromState(stat, type)
    }

    fun getLocalPathFromState(stat: StateID, type: String): String {
        return when (type) {
            AppNames.LOCAL_FILE_TYPE_FILE,
            AppNames.LOCAL_FILE_TYPE_TRANSFER
            -> "${dataParentPath(stat.accountId, type)}${stat.path}"
            AppNames.LOCAL_FILE_TYPE_THUMB,
            AppNames.LOCAL_FILE_TYPE_PREVIEW
            -> "${dataParentPath(stat.accountId, type)}${stat.file}"
            else -> throw IllegalStateException("Cannot create $type path for $stat")
        }
    }

    suspend fun registerLocalFile(
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

    fun unregisterLocalFile(stateID: StateID, type: String) {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()
        dao.delete(stateID.id, type)
    }

    /** This also checks that the file is in line with the index */
    fun getLocalFile(stateID: StateID, rTreeNode: RTreeNode, type: String): File? {
        val dao = treeNodeRepository.nodeDB(stateID).localFileDao()

        val rFile = dao.getFile(stateID.id, type) ?: let {
            Log.e(tag, "no record for [$type]: $stateID")
            return null
        }
        val parPath = dataParentPath(stateID.accountId, type)

        val file = File(parPath + File.separator + rFile.file)
        if (!file.exists()) {
            Log.e(tag, "could node find file at ${file.absolutePath}")
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

    fun getThumbPath(item: RTreeNode): String? {
// FIXME
        return null

//        return if (Str.empty(item.thumbFilename)) {
//            null
//        } else {
//            "${
//                dataParentPath(
//                    item.getStateID().accountId,
//                    AppNames.LOCAL_FILE_TYPE_THUMB
//                )
//            }${sep}${item.thumbFilename}"
//        }
    }

//    fun getOfflineThumbPath(item: RLiveOfflineRoot): String? {
//// FIXME
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

    fun getAccountBasePath(stateID: StateID, type: String): String {
        val dirName = treeNodeRepository.sessions[stateID.accountId]?.dirName
            ?: throw IllegalStateException("No record found for $stateID")
        val middle = sep + dirName
        return when (type) {
            AppNames.LOCAL_DIR_TYPE_CACHE ->
                appCacheDir + middle
            AppNames.LOCAL_DIR_TYPE_FILE ->
                appFilesDir + middle
            else -> throw IllegalStateException("Unknown base folder type: $type")
        }
    }

    fun createImageFile(stateID: StateID): File {
        val timestamp = getCurrentDateTime().asFormattedString("yyMMdd_HHmmss")
        val imgPath = dataParentPath(stateID.accountId, AppNames.LOCAL_FILE_TYPE_TRANSFER)
        // TODO do we really want a lazy creation for this base folder? or rather rely on a tree
        //    initialisation when the account is created
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
}
