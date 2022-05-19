package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "jobs")
data class RJob(
//    // A more precise reference to the job that has been running
//    @ColumnInfo(name = "template") val template: String,

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "job_id")
    var jobId: Long = 0L,
    // offline, clean, late upload
    @ColumnInfo(name = "type") val type: String,
    // New, processing, canceled, done, error
    @ColumnInfo(name = "status") var status: String? = null,
    // Message: might be an error but also a cancellation reason
    @ColumnInfo(name = "message") var message: String? = null,
    @ColumnInfo(name = "progress") var progress: Long = 0,
    // to compute the progress in pro-cent
    @ColumnInfo(name = "total_steps") var totalSteps: Long = 0,
    // this can be observed by the UI via live data together with the progress 
    @ColumnInfo(name = "progress_message") var progressMessage: String? = null,

    @ColumnInfo(name = "creation_ts") val creationTimestamp: Long,
    @ColumnInfo(name = "start_ts") var startTimestamp: Long = -1L,
    @ColumnInfo(name = "done_ts") var doneTimestamp: Long = -1L,

) {
    companion object {
        fun create(
            type: String,
            status: String? = AppNames.JOB_STATUS_NEW,
        ): RJob {
            return RJob(
                type = type,
                status = status,
                creationTimestamp = currentTimestamp(),
            )
        }
    }
}
