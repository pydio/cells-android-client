package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.transport.StateID
import java.util.*

@Entity(tableName = "transfers")
data class RTransfer(

    @PrimaryKey(autoGenerate = true)
    var transferId: Long = 0L,

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    // Download, upload... see AppNames for updated list of supported values
    @ColumnInfo(name = "type") val type: String,

    @ColumnInfo(name = "local_path") val localPath: String,

    @ColumnInfo(name = "byte_size") val byteSize: Long,

    @ColumnInfo(name = "mime") val mime: String,

    @ColumnInfo(name = "md5") var md5: String? = null,

    @ColumnInfo(name = "multipart") val multipart: Boolean = false,

    @ColumnInfo(name = "creation_ts") val creationTimestamp: Long,

    @ColumnInfo(name = "start_ts") var startTimestamp: Long = -1L,

    @ColumnInfo(name = "done_ts") var doneTimestamp: Long = -1L,

    @ColumnInfo(name = "error") var error: String? = null,

    @ColumnInfo(name = "progress") var progress: Long = 0,
) {

    fun getStateId(): StateID {
        return StateID.fromId(encodedState)
    }

    companion object {
        fun fromState(
            encodedState: String,
            type: String,
            path: String,
            byteSize: Long,
            mime: String
        ): RTransfer {
            return RTransfer(
                encodedState = encodedState,
                type = type,
                localPath = path,
                byteSize = byteSize,
                mime = mime,
                creationTimestamp = currentTimestamp(),
            )
        }
    }
}