package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.utils.currentTimestamp

@Entity(tableName = "transfer_cancellation")
data class RTransferCancellation(

    @PrimaryKey
    @ColumnInfo(name = "transfer_id")
    var transferId: Long,

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    @ColumnInfo(name = "request_ts") val requestTimestamp: Long,

    ) {

    companion object {
        fun cancel(
            encodedState: String,
            transferId: Long,
        ): RTransferCancellation {
            return RTransferCancellation(
                transferId = transferId,
                encodedState = encodedState,
                requestTimestamp = currentTimestamp(),
            )
        }
    }
}
