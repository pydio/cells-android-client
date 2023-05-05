package com.pydio.android.cells.ui.models

import com.pydio.cells.transport.StateID

data class BookmarkItem(
    val uuid: String,
    val mime: String,
    val eTag: String?,
    val name: String,
    val sortName: String?,
    val size: Long = -1L,
    val remoteModTs: Long = -1L,
    val isFolder: Boolean,
    val hasThumb: Boolean,
) {
    val appearsIn: MutableList<StateID> = mutableListOf()
    fun getStateID(): StateID = if (appearsIn.isEmpty()) StateID.NONE else appearsIn[0]
    val appearsInWorkspace: MutableMap<String, String> = mutableMapOf()

    override fun equals(other: Any?): Boolean {
        if (other !is BookmarkItem) {
            return false
        }
        return this.uuid == other.uuid
    }

    override fun hashCode(): Int {
        return this.uuid.hashCode()
    }
}