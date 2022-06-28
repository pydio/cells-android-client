package com.pydio.android.cells.transfer.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.api.SDKException
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.ByteBuffer

/**
 * Main entry point to use a custom loader with Glide:
 * when a given image is not found in Glide's internal cache, we first look in our cache
 * and if necessary, we download it (file is absent or remote node has changed).
 * Note that the diff is based on the remote modification timestamp and on the eTag.
 */
class CellsFileFetcher(private val model: String) : DataFetcher<ByteBuffer>, KoinComponent {

    // TODO implement cache cleaning for glide
//    // This method must be called on the main thread.
//    Glide.get(context).clearMemory()
//
//    Thread(Runnable {
//        // This method must be called on a background thread.
//        Glide.get(context).clearDiskCache()
//    }).start()

    private val logTag = CellsFileFetcher::class.simpleName

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private val transferService: TransferService by inject()

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ByteBuffer>) {
        dlScope.launch {
            val (stateId, type) = decodeModel(model)
            try {
                val (file, errMsg) = transferService.getFileForDisplay(stateId, type, null)
                file?.let {
                    // TODO rather use a stream
                    val bytes = it.readBytes()
                    val byteBuffer = ByteBuffer.wrap(bytes)
                    callback.onDataReady(byteBuffer)
                }
                if (Str.notEmpty(errMsg)) {
                    // TODO rather only rely on exception
                    callback.onLoadFailed(SDKException("could not get $type at $stateId: $errMsg"))
                }
            } catch (e: Exception) {
                Log.e(logTag, "error while trying to get $type at $stateId: ${e.message}")
                callback.onLoadFailed(e)
            }
        }
    }

    override fun cleanup() {
        // TODO
    }

    override fun cancel() {
        // TODO
    }

    override fun getDataClass(): Class<ByteBuffer> {
        return ByteBuffer::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}
