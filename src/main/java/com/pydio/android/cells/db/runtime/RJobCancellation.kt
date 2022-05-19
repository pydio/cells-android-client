package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "job_cancellation")
data class RJobCancellation(

    @PrimaryKey
    @ColumnInfo(name = "job_id")
    var jobId: Long,

    @ColumnInfo(name = "request_ts") val requestTimestamp: Long,

    ) {

    companion object {
        fun cancel(
            jobId: Long,
        ): RJobCancellation {
            return RJobCancellation(
                jobId = jobId,
                requestTimestamp = currentTimestamp(),
            )
        }
    }
}
