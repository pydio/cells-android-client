package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "transfer_cancellation")
data class RTransferCancellation(
    @PrimaryKey
    // Unique ID of the transfer to stop
    @ColumnInfo(name = "transfer_id") val transferId: Long,
    // Corresponding target state (mainly for logging purposes)
    @ColumnInfo(name = "encoded_state") val encodedState: String,
    // Owner of the cancellation event (user, worker or system)
    @ColumnInfo(name = "owner") val owner: String,
    // Timestamp for the cancellation event
    @ColumnInfo(name = "request_ts") val requestTimestamp: Long,
    // By default only job children are cancelled, not the ancestor
    @ColumnInfo(name = "also_stop_ancestors") val alsoStopAncestors: Boolean,
) {

    companion object {
        fun cancel(
            transferId: Long,
            encodedState: String,
            owner: String,
            alsoStopAncestors: Boolean = false
        ): RTransferCancellation {
            return RTransferCancellation(
                transferId = transferId,
                encodedState = encodedState,
                owner = owner,
                alsoStopAncestors = alsoStopAncestors,
                requestTimestamp = currentTimestamp(),
            )
        }
    }
}
