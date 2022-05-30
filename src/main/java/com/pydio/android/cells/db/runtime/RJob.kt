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
    // The template: offline, clean, late upload, migration
    @ColumnInfo(name = "template") val template: String,
    // Who triggered the job: worker, user, external-<app-id>
    @ColumnInfo(name = "owner") val owner: String,
    // New, processing, canceled, done, error
    @ColumnInfo(name = "status") var status: String? = null,
    // Message: might be an error but also a cancellation reason
    @ColumnInfo(name = "message") var message: String? = null,
    // Observable value for progress listeners
    @ColumnInfo(name = "progress") var progress: Long = 0,
    // Observable status for the end user
    @ColumnInfo(name = "progress_msg") var progressMessage: String? = null,
    // To compute the progress in pro-cent
    @ColumnInfo(name = "total") var total: Long = -1,
    // Timestamps
    @ColumnInfo(name = "creation_ts") val creationTimestamp: Long,
    @ColumnInfo(name = "start_ts") var startTimestamp: Long = -1L,
    @ColumnInfo(name = "update_ts") var updateTimestamp: Long = -1L,
    @ColumnInfo(name = "done_ts") var doneTimestamp: Long = -1L,

    ) {

    fun isFail(): Boolean {
        return status == AppNames.JOB_STATUS_ERROR ||
                status == AppNames.JOB_STATUS_TIMEOUT ||
                status == AppNames.JOB_STATUS_CANCELLED
    }

    companion object {
        fun create(
            owner: String,
            template: String,
            label: String,
            parentId: Long = 0,
            status: String? = AppNames.JOB_STATUS_NEW,
        ): RJob {
            return RJob(
                owner = owner,
                parentId = parentId,
                label = label,
                template = template,
                status = status,
                creationTimestamp = currentTimestamp(),
            )
        }
    }
}
