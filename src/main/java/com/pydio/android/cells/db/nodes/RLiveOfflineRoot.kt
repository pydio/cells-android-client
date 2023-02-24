package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.TypeConverters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.Converters
import com.pydio.cells.transport.StateID

@DatabaseView(
    "SELECT offline_roots.encoded_state, " +
            "offline_roots.uuid, " +
            "offline_roots.status, " +
            "offline_roots.local_mod_ts, " +
            "offline_roots.last_check_ts, " +
            "offline_roots.message, " +
            "tree_nodes.mime, " +
            "tree_nodes.name, " +
            "tree_nodes.size, " +
            "tree_nodes.etag, " +
            "tree_nodes.remote_mod_ts, " +
            "tree_nodes.flags, " +
            "offline_roots.sort_name " +
            "FROM offline_roots INNER JOIN tree_nodes " +
            "ON offline_roots.encoded_state = tree_nodes.encoded_state"
)
@TypeConverters(Converters::class)
data class RLiveOfflineRoot(

    @ColumnInfo(name = "uuid") val uuid: String?,

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    @ColumnInfo(name = "mime") val mime: String,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "status") val status: String,

    @ColumnInfo(name = "remote_mod_ts") var remoteModTS: Long,

    @ColumnInfo(name = "local_mod_ts") val localModTs: Long = 0L,

    @ColumnInfo(name = "last_check_ts") val lastCheckTs: Long = 0L,

    @ColumnInfo(name = "message") val message: String?,

    @ColumnInfo(name = "sort_name") val sortName: String?,

    @ColumnInfo(name = "size") val size: Long = -1L,

    @ColumnInfo(name = "etag") val etag: String?,

    @ColumnInfo(name = "flags") val flags: Int,

    ) {

    fun getStateID(): StateID {
        return StateID.fromId(encodedState)
    }

    fun isFolder(): Boolean {
        return RTreeNode.isFolderFromMime(mime)
    }

    fun hasThumb(): Boolean {
        return isFlag(AppNames.FLAG_HAS_THUMB)
    }

    fun isBookmarked(): Boolean {
        return isFlag(AppNames.FLAG_BOOKMARK)
    }

    fun isShared(): Boolean {
        return isFlag(AppNames.FLAG_SHARE)
    }

    private fun isFlag(flag: Int): Boolean {
        return flags and flag == flag
    }

    fun isContentEquals(
        newItem: RLiveOfflineRoot
    ): Boolean {
        // TODO better check
        return lastCheckTs == newItem.lastCheckTs
                && localModTs == newItem.localModTs
    }
}
