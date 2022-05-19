package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "jobs")
data class RJob(

    // Main id
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "job_id") var jobId: Long = 0L,
    // Optional reference to a parent job
    @ColumnInfo(name = "parent_id") var parentId: Long = 0L,
    // Human readable label
    @ColumnInfo(name = "label") val label: String,
    // the template: offline, clean, late upload, migration
    @ColumnInfo(name = "template") val template: String,
    // New, processing, canceled, done, error
    @ColumnInfo(name = "status") var status: String? = null,
    // Message: might be an error but also a cancellation reason
    @ColumnInfo(name = "message") var message: String? = null,
    // Observable value for progress listeners
    @ColumnInfo(name = "progress") var progress: Long = 0,
    // To compute the progress in pro-cent
    @ColumnInfo(name = "progress_max") var progressMax: Long = -1,
    // Observable status for the end user
    @ColumnInfo(name = "progress_message") var progressMessage: String? = null,
    // Timestamps
    @ColumnInfo(name = "creation_ts") val creationTimestamp: Long,
    @ColumnInfo(name = "start_ts") var startTimestamp: Long = -1L,
    @ColumnInfo(name = "done_ts") var doneTimestamp: Long = -1L,

    ) {

    companion object {
        fun create(
            label: String,
            template: String,
            parentId: Long = 0,
            status: String? = AppNames.JOB_STATUS_NEW,
        ): RJob {
            return RJob(
                parentId = parentId,
                label = label,
                template = template,
                status = status,
                creationTimestamp = currentTimestamp(),
            )
        }
    }
}
