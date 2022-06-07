package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames
import com.pydio.cells.transport.StateID

@Entity(tableName = "offline_roots")
data class ROfflineRoot(

    @PrimaryKey
    @ColumnInfo(name = "encoded_state") var encodedState: String,

    @ColumnInfo(name = "uuid") val uuid: String,

    @ColumnInfo(name = "status") var status: String,

    @ColumnInfo(name = "local_mod_ts") var localModificationTS: Long = 0L,

    @ColumnInfo(name = "last_check_ts") var lastCheckTS: Long = 0L,

    @ColumnInfo(name = "message") var message: String?,

    @ColumnInfo(name = "sort_name") var sortName: String? = null,

    // Can be: internal or external, optionally with an index
    // TODO: we only support storage in the app files dir for the time being.
    @ColumnInfo(name = "storage") var storage: String = AppNames.OFFLINE_STORAGE_INTERNAL,
) {

    fun getStateID(): StateID {
        return StateID.fromId(encodedState)
    }

    companion object {
        fun fromTreeNode(treeNode: RTreeNode): ROfflineRoot {
            return ROfflineRoot(
                encodedState = treeNode.encodedState,
                uuid = treeNode.uuid,
                status = AppNames.OFFLINE_STATUS_NEW,
                localModificationTS = 0,
                lastCheckTS = 0,
                message = null,
                sortName = treeNode.sortName,
            )
        }
    }
}
