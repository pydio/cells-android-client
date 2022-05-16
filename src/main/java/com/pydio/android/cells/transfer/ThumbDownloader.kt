package com.pydio.android.cells.transfer

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.FileService
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.FileNodeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.coroutineContext

class ThumbDownloader(
    private val client: Client,
    private val nodeDB: TreeNodeDB,
    private val thumbSize: Int = 100,
) : KoinComponent {

    private val logTag = ThumbDownloader::class.java.simpleName

    private val doneChannel = Channel<Boolean>()
    private val queue = Channel<String>()

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private val fileService: FileService by inject()
    private val accountService: AccountService by inject()

    private suspend fun download(encodedState: String) {

        val state = StateID.fromId(encodedState)
        val rNode = nodeDB.treeNodeDao().getNode(encodedState)
        if (rNode == null) {
            // No node found, aborting
            Log.w(logTag, "No node found for $state, aborting thumb DL")
            return
        }

        val node = FileNode()
        node.properties = rNode.properties
        node.meta = rNode.meta

        val parentFolder =
            fileService.dataParentPath(state.accountId, AppNames.LOCAL_FILE_TYPE_THUMB)
        try {
            client.getThumbnail(state, node, File(parentFolder), thumbSize)?.let {
                if (!client.isLegacy) {
                    handleOrientation(rNode, parentFolder + File.separator + it)
                }

                rNode.thumbFilename = it
                nodeDB.treeNodeDao().update(rNode)
            }
        } catch (e: SDKException) {
            Log.w(logTag, "could not download thumbnail for $state, error #${e.code}: ${e.message}")
            // accountService.notifyError(state, e.code)
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

    private fun targetPath(accountId: String, targetName: String): String {
        val thumbParPath = fileService.dataParentPath(accountId, AppNames.LOCAL_FILE_TYPE_THUMB)
        return thumbParPath + File.separator + targetName
    }


    suspend fun orderThumbDL(url: String) {
        Log.d(logTag, "DL Thumb for $url")
        queue.send(url)
    }

    suspend fun allDone() {
        doneChannel.send(true)
    }

    private suspend fun processThumbDL() {
        for (msg in queue) { // iterate over incoming messages
            when (msg) {
                "done" -> {
                    Log.d(logTag, "Received done message, forwarding to done channel.")
                    doneChannel.send(true)
                    return
                }
                else -> {
                    download(msg)
                }
            }
        }
    }

    init {
        initialize()
    }

    private fun initialize() {
        dlScope.launch { waitForDone() }
        dlScope.launch { processThumbDL() }
    }

    private suspend fun waitForDone() {
        for (msg in doneChannel) {
            Log.i(logTag, "Finished processing the queue, exiting...")
            queue.close()
            doneChannel.close()
            break
        }
        coroutineContext.cancelChildren()
    }
}
