package com.pydio.android.cells.transfer

import android.util.Log
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.coroutineContext

class ThumbDownloader : KoinComponent {
    //    (
//     private val client: Client,
//     private val nodeDB: TreeNodeDB,
//     private val thumbSize: Int = 100,
// ) : KoinComponent {
    private val logTag = ThumbDownloader::class.java.simpleName

    private val doneChannel = Channel<Boolean>()
    private val queue = Channel<String>()

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    // private val fileService: FileService by inject()
    private val transferService: TransferService by inject()
    // private val accountService: AccountService by inject()

    private suspend fun download(encoded: String) {

        val res = decodeModel(encoded)
        try {
            transferService.getOrDownloadFile(res.first, res.second)
        } catch (e: SDKException) {
            Log.w(
                logTag,
                "could not download ${res.second} for ${res.first}, error #${e.code}: ${e.message}"
            )
            // accountService.notifyError(state, e.code)
        }
    }

    suspend fun orderThumbDL(url: String, type: String) {
        val encoded = "$type:$url"
        Log.d(logTag, "DL Thumb for $url")
        queue.send(encoded)
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

    private fun decodeModel(model: String): Pair<StateID, String> {
        val type = model.substring(0, model.indexOf(":"))
        val encoded = model.substring(model.indexOf(":") + 1)
        return Pair(StateID.fromId(encoded), type)
    }
}
