package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.TypeConverters
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.db.Converters

@DatabaseView(
    "SELECT offline_roots.encoded_state, " +
            "offline_roots.uuid, " +
            "offline_roots.status, " +
            "offline_roots.local_mod_ts, " +
            "offline_roots.last_check_ts, " +
            "offline_roots.message, " +
            "offline_roots.sort_name, " +
            "tree_nodes.mime, " +
            "tree_nodes.name, " +
            "tree_nodes.size, " +
            "tree_nodes.thumb, " +
            "tree_nodes.localPath " +
            "FROM offline_roots INNER JOIN tree_nodes " +
            "ON offline_roots.encoded_state = tree_nodes.encoded_state"
)
@TypeConverters(Converters::class)
data class RLiveOfflineRoot(

    @ColumnInfo(name = "uuid") val uuid: String?,

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    @ColumnInfo(name = "mime") val mime: String,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "status") var status: String,

    @ColumnInfo(name = "local_mod_ts") val localModTs: Long = 0L,

    @ColumnInfo(name = "last_check_ts") var lastCheckTs: Long = 0L,

    @ColumnInfo(name = "message") var message: String?,

    @ColumnInfo(name = "sort_name") var sortName: String?,

    @ColumnInfo(name = "size") var size: Long = -1L,

    @ColumnInfo(name = "thumb") val thumbFilename: String?,

    @ColumnInfo(name = "localPath") val localPath: String?,
) {

    fun getStateID(): StateID {
        return StateID.fromId(encodedState)
    }

    fun isFolder(): Boolean {
        return RTreeNode.isFolderFromMime(mime)
    }

    fun isContentEquals(
        newItem: RLiveOfflineRoot
    ): Boolean {
        // TODO better check
        return lastCheckTs == newItem.lastCheckTs
                && localModTs == newItem.localModTs
    }
}
