package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "logs")
data class RLog(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    var logId: Long = 0L,

    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "tag") val tag: String,
    @ColumnInfo(name = "message") val message: String? = null,
    @ColumnInfo(name = "caller_id") val callerId: String? = null,

    ) {
    companion object {
        fun create(
            level: String,
            tag: String,
            message: String,
            callerId: String?
        ): RLog {
            return RLog(
                level = when (level) {
                    "Trace" -> 0
                    "Debug" -> 2
                    "Info" -> 4
                    "Warning" -> 6
                    "Error" -> 8
                    "Fatal" -> 10
                    else -> 99
                },
                tag = tag,
                message = message,
                callerId = callerId,
                timestamp = currentTimestamp(),
            )
        }
    }
}
