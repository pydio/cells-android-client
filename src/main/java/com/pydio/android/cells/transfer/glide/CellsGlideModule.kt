package com.pydio.android.cells.transfer.glide

import android.content.Context
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.pydio.android.cells.transfer.glide.CellsModelLoaderFactory
import java.nio.ByteBuffer

@GlideModule
class CellsGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(String::class.java, ByteBuffer::class.java, CellsModelLoaderFactory())
    }
}
