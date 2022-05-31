package com.pydio.android.cells.db.runtime

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames

@Entity(tableName = "network_info")
data class RNetworkInfo(

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,

    // unknown, no_internet, metered, ok
    @ColumnInfo(name = "status") var status: String = AppNames.NETWORK_STATUS_UNKNOWN,

    @ColumnInfo(name = "last_checked") var lastCheckedTS: Long = -1L,

    @ColumnInfo(name = "last_response_code") var lastResponseCode: Int = 200,

    @ColumnInfo(name = "last_response_msg") var lastResponseMsg: String? = null,

    ){

    fun isOnline() = status == AppNames.NETWORK_STATUS_OK

    fun isOffline() = status == AppNames.NETWORK_STATUS_NO_INTERNET
}
