package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "logs")
data class RLog(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    var logId: Long = 0L,

    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "tag") val tag: String?,
    @ColumnInfo(name = "message") val message: String? = null,
    @ColumnInfo(name = "caller_id") val callerId: String? = null,
) {

    fun getLevelString(): String = when (level) {
        1 -> AppNames.FATAL
        2 -> AppNames.ERROR
        3 -> AppNames.WARNING
        4 -> AppNames.INFO
        5 -> AppNames.DEBUG
        6 -> AppNames.TRACE
        else -> "unknown level: $level"
    }

    companion object {

        fun create(level: String, tag: String?, message: String, callerId: String?): RLog {
            return RLog(
                level = when (level) {
                    AppNames.FATAL -> 1
                    AppNames.ERROR -> 2
                    AppNames.WARNING -> 3
                    AppNames.INFO -> 4
                    AppNames.DEBUG -> 5
                    AppNames.TRACE -> 6
                    else -> 99
                },
                tag = tag,
                message = message,
                callerId = callerId,
                timestamp = currentTimestamp(),
            )
        }

        fun info(tag: String?, message: String, callerId: String?): RLog {
            return create(AppNames.INFO, tag, message, callerId)
        }
    }
}
