package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.AppNames

@Entity(tableName = "offline_roots")
data class ROfflineRoot(

    @PrimaryKey
    @ColumnInfo(name = "uuid") val uuid: String,

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    @ColumnInfo(name = "status") var status: String,

    @ColumnInfo(name = "local_mod_ts") var localModificationTS: Long = 0L,

    @ColumnInfo(name = "last_check_ts") var lastCheckTS: Long = 0L,

    @ColumnInfo(name = "message") var message: String?,

    @ColumnInfo(name = "sort_name") var sortName: String? = null,

    // Can be: internal or external, optionally with an index
    @ColumnInfo(name = "storage_key") var localFileType: String = AppNames.LOCAL_FILE_TYPE_NONE,
) {

    fun getStateID(): StateID {
        return StateID.fromId(encodedState)
    }

    companion object {
        fun fromTreeNode(treeNode: RTreeNode): ROfflineRoot {
            return ROfflineRoot(
                encodedState = treeNode.encodedState,
                uuid = treeNode.uuid,
                status = "new",
                localModificationTS = 0,
                lastCheckTS = 0,
                message = null,
                sortName = treeNode.sortName,
                // TODO: we only support storage in the app files dir for the time being.
                localFileType = "internal",
            )
        }
    }
}
