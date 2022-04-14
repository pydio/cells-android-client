package com.pydio.android.cells.transfer

import android.util.Log
import com.google.gson.Gson
import com.pydio.cells.api.Client
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Str
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import com.pydio.android.cells.db.nodes.TreeNodeDB
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.coroutines.coroutineContext

class ThumbDownloader(
    private val client: Client,
    private val nodeDB: TreeNodeDB,
    private val filesDir: File,
    private val thumbSize: Int = 100,
) {

    private val logTag = ThumbDownloader::class.java.simpleName

    private val doneChannel = Channel<Boolean>()
    private val queue = Channel<String>()

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private fun download(encodedState: String) {
        val state = StateID.fromId(encodedState)

        val rNode = nodeDB.treeNodeDao().getNode(encodedState)
        if (rNode == null) {
            // No node found, aborting
            Log.w(logTag, "No node found for $state, aborting thumb DL")
            return
        }
        // Prepare a "light" FileNode that is used to get the thumbnail
        // This will be refactored once we have reached MVP
        val node = FileNode()
        node.setProperty(SdkNames.NODE_PROPERTY_WORKSPACE_SLUG, state.workspace)
        node.setProperty(SdkNames.NODE_PROPERTY_PATH, state.file)

        if (!client.isLegacy && rNode.meta.containsKey(SdkNames.NODE_PROPERTY_REMOTE_THUMBS)) {
            node.setProperty(
                SdkNames.NODE_PROPERTY_REMOTE_THUMBS,
                rNode.meta.getProperty(SdkNames.NODE_PROPERTY_REMOTE_THUMBS)
            )
        }

        val targetName = targetName(node)
        if (Str.empty(targetName)) {
            Log.e(logTag, "No target file found for $state, aborting...")
//            Log.d(tag, ".... Listing meta to debug:")
//            for (meta in rNode.meta) {
//                Log.d(tag, "- ${meta.key} : ${meta.value}")
//            }
            return
        }

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(File(targetPath(targetName!!)))
            client.getPreviewData(node, thumbSize, out)

            // Then set the thumb name in current node
            rNode.thumbFilename = targetName
            nodeDB.treeNodeDao().update(rNode)
        } catch (se: SDKException) { // Could not retrieve thumb, failing silently for the end user
            Log.e(logTag, "Could not retrieve thumb for " + state + ": " + se.message)
        } catch (ioe: IOException) {
            // TODO Could not write the thumb in the local cache, we should notify the user
            Log.e(
                logTag,
                "could not write newly downloaded thumb to the local device for "
                        + state + ": " + ioe.message
            )
        } finally {
            IoHelpers.closeQuietly(out)
        }
    }

    private fun targetPath(targetName: String): String {
        return filesDir.absolutePath + File.separator + targetName
    }

    private fun targetName(currNode: FileNode): String? {
        if (client.isLegacy) {
            return UUID.randomUUID().toString() + ".jpg"
        } else {
            // FIXME this code has been copied from the Cells Client for the MVP
            val remoteThumbsJson = currNode.getProperty(SdkNames.NODE_PROPERTY_REMOTE_THUMBS)
            if (Str.empty(remoteThumbsJson)) {
                Log.w(logTag, "No JSON thumb metadata found, aborting...")
                return null
            }
            val gson = Gson()

            val thumbs = gson.fromJson(remoteThumbsJson, Map::class.java)
            var thumbPath: String? = null
            for ((key, value) in thumbs) {
                if (thumbPath == null) {
                    thumbPath = value as String
                }
                val size = (key as String).toInt()
                if (size > 0 && size >= thumbSize) {
                    return value as String
                }
            }
            return thumbPath
        }
    }

    suspend fun orderThumbDL(url: String) {
        println("DL Thumb for $url")
        queue.send(url)
    }

    suspend fun allDone() {
        doneChannel.send(true)
    }

    private suspend fun processThumbDL() {
        for (msg in queue) { // iterate over incoming messages
            when (msg) {
                "done" -> {
                    println("Received done message, forwarding to done channel.")
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
        filesDir.mkdirs()
        dlScope.launch { waitForDone() }
        dlScope.launch { processThumbDL() }
    }

    private suspend fun waitForDone() {
        for (msg in doneChannel) {
            println("Finished processing the queue, exiting...")
            queue.close()
            doneChannel.close()
            break
        }
        coroutineContext.cancelChildren()
    }
}
