package com.pydio.android.cells.transfer.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import java.nio.ByteBuffer

/**
 * Loads a local file from an encoded state ID with a type.
 * If the file is not found locally, it is downloaded.
 *
 * Note that the model also contains the remote node eTAG:
 * if the remote image has changed, the eTAG is impacted and thus the model changes,
 * triggering cache invalidation for this image in Glide's layers.
 */
class CellsModelLoader : ModelLoader<String, ByteBuffer> {

    private val logTag = "CellsModelLoader"
    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<ByteBuffer>? {
        return ModelLoader.LoadData(ObjectKey(model), CellsFileFetcher(model))
    }

    override fun handles(model: String): Boolean {
        // TODO better validation?
        return try {
            val res = decodeModel(model)
            Str.notEmpty(res.second)
        } catch (e: Exception) {
            Log.e(logTag, "Unexpected exception while handling $model: ${e.message}.")
            false
        }
    }
}
 