package com.pydio.android.cells.db.nodes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.pydio.android.cells.db.Converters

@Dao
@TypeConverters(Converters::class)
interface TransferDao {

    // MAIN TRANSFER OBJECT

    @Insert
    fun insert(transfer: RTransfer): Long

    @Update
    fun update(transfer: RTransfer)

    @Query("SELECT * FROM transfers WHERE encoded_state = :stateId LIMIT 1")
    fun getByState(stateId: String): RTransfer?

    @Query("SELECT * FROM transfers WHERE transfer_id IN (:ids)")
    fun getCurrents(ids: Set<Long>): LiveData<List<RTransfer>>

    @Query("SELECT * FROM transfers WHERE transfer_id = :transferID LIMIT 1")
    fun getById(transferID: Long): RTransfer?

    @Query("SELECT * FROM transfers WHERE job_id = :jobID")
    fun getByJobId(jobID: Long): LiveData<List<RTransfer>>

    @Query("SELECT * FROM transfers WHERE transfer_id = :transferID LIMIT 1")
    fun getLiveById(transferID: Long): LiveData<RTransfer?>

    @Query("SELECT * FROM transfers WHERE encoded_state = :encodedState LIMIT 1")
    fun getLiveByState(encodedState: String): LiveData<RTransfer?>

    @Query("SELECT * FROM transfers WHERE start_ts = -1")
    fun getAllNew(): List<RTransfer>

    @Query("SELECT COUNT(*) FROM transfers")
    fun getTransferCount(): Long

    @Query("SELECT * FROM transfers ORDER BY start_ts DESC")
    fun getActiveTransfers(): LiveData<List<RTransfer>?>

    @Query("DELETE FROM transfers WHERE done_ts = -1 AND update_ts < :staleLimit ")
    fun clearStaleTransfers(staleLimit: Long)

    @Query("DELETE FROM transfers WHERE done_ts > 0")
    fun clearTerminatedTransfers()

    @Query("DELETE FROM transfers WHERE transfer_id = :transferID")
    fun deleteTransfer(transferID: Long)

    @RawQuery(observedEntities = [RTransfer::class])
    fun transferQuery(query: SupportSQLiteQuery): LiveData<List<RTransfer>>

    // TRANSFER CANCELLATION

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cancellation: RTransferCancellation)

    @Query("SELECT * FROM transfer_cancellation WHERE transfer_id = :transferID LIMIT 1")
    fun hasBeenCancelled(transferID: Long): RTransferCancellation?

    @Query("DELETE FROM transfer_cancellation WHERE transfer_id = :transferID")
    fun deleteCancellation(transferID: Long)

    @Query("DELETE FROM transfer_cancellation WHERE transfer_id = :transferID")
    fun ackCancellation(transferID: Long)
}
