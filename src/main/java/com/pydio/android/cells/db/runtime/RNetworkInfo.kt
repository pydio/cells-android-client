package com.pydio.android.cells.db.runtime

//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//import com.pydio.android.cells.AppNames
//import com.pydio.android.cells.utils.currentTimestamp
//
///**
// * Experimental class to try a cache of the network status. Will probably disappear soon:
// * Do not rely on it.
// */
//@Entity(tableName = "network_info")
//data class RNetworkInfo(
//
//    @PrimaryKey(autoGenerate = true)
//    var id: Long = 0L,
//
//    // unknown, no_internet, metered, ok
//    @ColumnInfo(name = "status") var status: String = AppNames.NETWORK_TYPE_UNKNOWN,
//
//    @ColumnInfo(name = "last_checked") var lastCheckedTS: Long = -1L,
//
//    @ColumnInfo(name = "last_response_code") var lastResponseCode: Int = 200,
//
//    @ColumnInfo(name = "last_response_msg") var lastResponseMsg: String? = null,
//
//    ) {
//
//    fun isOnline() = status == AppNames.NETWORK_TYPE_UNMETERED
//
//    fun isOffline() = status == AppNames.NETWORK_TYPE_UNAVAILABLE
//
//    companion object {
//        fun create(
//            status: String?,
//            lastResponseCode: Int,
//            lastResponseMsg: String?
//        ): RNetworkInfo {
//            return RNetworkInfo(
//                status = status ?: AppNames.NETWORK_TYPE_UNKNOWN,
//                lastResponseCode = lastResponseCode,
//                lastResponseMsg = lastResponseMsg,
//                lastCheckedTS = currentTimestamp(),
//            )
//        }
//    }
//}
