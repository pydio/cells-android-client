package com.pydio.android.cells.db.runtime

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.pydio.android.cells.db.Converters

@Dao
@TypeConverters(Converters::class)
interface JobDao {

    // MAIN JOB OBJECT

    @Insert
    fun insert(job: RJob): Long

    @Update
    fun update(job: RJob)

    @Query("SELECT * FROM jobs WHERE job_id = :jobId LIMIT 1")
    fun getById(jobId: Long): RJob?

    @Query("SELECT * FROM jobs WHERE job_id = :jobId LIMIT 1")
    fun getLiveById(jobId: Long): LiveData<RJob?>

    @Query("SELECT * FROM jobs WHERE start_ts = -1")
    fun getAllNew(): List<RJob>

    @Query("SELECT * FROM jobs ORDER BY start_ts DESC")
    fun getActiveTransfers(): LiveData<List<RJob>?>

    @Query("DELETE FROM jobs WHERE done_ts > 0")
    fun clearTerminatedTransfers()

    @Query("DELETE FROM jobs WHERE job_id = :jobId")
    fun deleteTransfer(jobId: Long)

    // JOB CANCELLATION
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cancellation: RJobCancellation)

    @Query("SELECT * FROM job_cancellation WHERE job_id = :jobId LIMIT 1")
    fun hasBeenCancelled(jobId: Long): RJobCancellation?

    @Query("DELETE FROM job_cancellation WHERE job_id = :jobId")
    fun deleteCancellation(jobId: Long)

}