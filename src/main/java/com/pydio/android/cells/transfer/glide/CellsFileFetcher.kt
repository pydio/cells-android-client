package com.pydio.android.cells.transfer.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.services.TransferService
import com.pydio.android.cells.utils.showMessage
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

    private val logTag = CellsFileFetcher::class.simpleName

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private val transferService: TransferService by inject()

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ByteBuffer>) {
        // Log.e(logTag, "About to load the file...")

        val (stateId, type) = decodeModel(model)
        dlScope.launch {

            val (file, errMsg) = transferService.getFileForDisplay(stateId, type, null)
            file?.let {
                // TODO rather use a stream
                // Log.e(logTag, "Got a file...")
                val bytes = it.readBytes()
                val byteBuffer = ByteBuffer.wrap(bytes)
                callback.onDataReady(byteBuffer)
            }
            // TODO rather display a boken image with the error message
            if (Str.notEmpty(errMsg)) {
                Log.e(logTag, "could not get $type at $stateId: $errMsg")
//                with(Dispatchers.Main) {
//                    showMessage(CellsApp.instance.applicationContext, errMsg!!)
//                }
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
