package com.pydio.android.cells.transfer.glide

import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.cells.transport.StateID

fun encodeModel(rTreeNode: RTreeNode, type: String): String {
    return encodeModel(rTreeNode.encodedState, rTreeNode.etag, type)
}

// Enable encoded model for RLiveOfflineRoot objects.
fun encodeModel(encodedState: String, eTag: String?, type: String): String {
    // We pre-pend the model with the eTag, so that it changes when the image changes
    return (eTag ?: "none") + ":" + type + ":" + encodedState
}

fun decodeModel(encoded: String): Pair<StateID, String> {
    // We remove the prefix that we do not use:
    //Log.d("decodeModel", "Encoded: $encoded")
    val model = encoded.substring(encoded.indexOf(":") + 1)
    val type = model.substring(0, model.indexOf(":"))
    val encodedState = model.substring(model.indexOf(":") + 1)
    // Log.d("decodeModel", "Decoded: ${StateID.fromId(encodedState)} - $type")
    return Pair(StateID.fromId(encodedState), type)
}
