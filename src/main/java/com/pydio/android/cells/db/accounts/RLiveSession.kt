package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.TypeConverters
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.db.Converters

@DatabaseView(
    "SELECT sessions.account_id, " +
            "sessions.lifecycle_state, " +
            "sessions.dir_name, " +
            "sessions.db_name, " +
            "accounts.url, " +
            "accounts.username, " +
            "accounts.auth_status, " +
            "accounts.tls_mode, " +
            "accounts.is_legacy, " +
            "accounts.server_label, " +
            "accounts.welcome_message " +
            "FROM sessions INNER JOIN accounts " +
            "ON sessions.account_id = accounts.account_id"
)
@TypeConverters(Converters::class)
data class RLiveSession(
    @ColumnInfo(name = "account_id") val accountID: String,
    @ColumnInfo(name = "lifecycle_state") val lifecycleState: String,
    @ColumnInfo(name = "dir_name") var dirName: String,
    @ColumnInfo(name = "db_name") var dbName: String,

    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "auth_status") var authStatus: String,
    @ColumnInfo(name = "tls_mode") var tlsMode: Int,
    @ColumnInfo(name = "is_legacy") var isLegacy: Boolean,
    @ColumnInfo(name = "server_label") val serverLabel: String?,
    @ColumnInfo(name = "welcome_message") val welcomeMessage: String?,

    // TODO Add a simple state that stores the current network info and put it in this view

   // TODO implement a helper method that returns current session "mode" depending on:
    //    - network info
    //    - lifecycle state
    //    - auth status
) {
    fun getStateID(): StateID {
        return StateID.fromId(accountID)
    }
}

//// Not very useful for the time being, kept here for the pattern
//fun List<RLiveSession>.asDomainModel(): List<RLiveSession> {
//    return map {
//        it
//    }
//}
