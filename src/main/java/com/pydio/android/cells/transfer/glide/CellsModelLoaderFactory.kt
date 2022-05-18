package com.pydio.android.cells.transfer.glide

import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.nio.ByteBuffer

class CellsModelLoaderFactory : ModelLoaderFactory<String, ByteBuffer> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, ByteBuffer> {
        return CellsModelLoader()
    }

    override fun teardown() {
        // Do nothing.
    }
}
