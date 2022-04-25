package com.pydio.android.cells.transfer

import android.util.Log
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.services.TransferService
import kotlin.coroutines.coroutineContext

class FileDownloader : KoinComponent {

    private val logTag = FileDownloader::class.java.simpleName
    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private val transferService: TransferService by inject()

    private val doneChannel = Channel<Boolean>()
    private val queue = Channel<String>()

    private suspend fun download(encodedState: String) {
        val state = StateID.fromId(encodedState)
        val result = transferService.prepareDownload(state, AppNames.LOCAL_FILE_TYPE_OFFLINE)
        if (result.first != null && Str.empty(result.second)) {
            transferService.downloadFile(state, result.first!!)
        }
    }

//    private fun targetPath(state: StateID): String {
//        return fileService.getLocalPathFromState(state, AppNames.LOCAL_FILE_TYPE_OFFLINE)
//    }

    suspend fun orderFileDL(url: String) {
        Log.d(logTag, "Ordered File DL for $url")
        queue.send(url)
    }

    suspend fun allDone() {
        doneChannel.send(true)
    }

    private suspend fun processFileDL() {
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
        dlScope.launch { processFileDL() }
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
