package com.pydio.android.cells.transfer.glide

import android.util.Log
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.pydio.android.cells.services.TransferService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.ByteBuffer

class CellsFileFetcher(private val model: String) : DataFetcher<ByteBuffer>, KoinComponent {

    private val logTag = CellsFileFetcher::class.simpleName

    private var dlJob = Job()
    private val dlScope = CoroutineScope(Dispatchers.IO + dlJob)

    private val transferService: TransferService by inject()


    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in ByteBuffer>) {
        Log.e(logTag, "About to load the file...")

        val pair = decodeModel(model)
        dlScope.launch {

            transferService.getOrDownloadFile(pair.first, pair.second)?.let {
                // TODO rather use a stream

                Log.e(logTag, "Got a file...")
                var bytes = it.readBytes()
                val byteBuffer = ByteBuffer.wrap(bytes)
                callback.onDataReady(byteBuffer)
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
