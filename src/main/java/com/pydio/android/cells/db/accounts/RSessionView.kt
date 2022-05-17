package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import androidx.room.TypeConverters
import com.pydio.android.cells.db.Converters
import com.pydio.cells.transport.StateID
import java.util.*

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
            "accounts.properties " +
//            "accounts.welcome_message " +
            "FROM sessions INNER JOIN accounts " +
            "ON sessions.account_id = accounts.account_id"
)
@TypeConverters(Converters::class)
data class RSessionView(
    @ColumnInfo(name = "account_id") val accountID: String,
    @ColumnInfo(name = "lifecycle_state") val lifecycleState: String,
    @ColumnInfo(name = "dir_name") var dirName: String,
    @ColumnInfo(name = "db_name") var dbName: String,

    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "auth_status") var authStatus: String,
    @ColumnInfo(name = "tls_mode") var tlsMode: Int,
    @ColumnInfo(name = "is_legacy") var isLegacy: Boolean,
    @ColumnInfo(name = "properties") var properties: Properties,

//    @ColumnInfo(name = "server_label") val serverLabel: String?,
//    @ColumnInfo(name = "welcome_message") val welcomeMessage: String?,
) {
    fun skipVerify() = tlsMode != 0

    fun serverLabel(): String? {
        if (properties.containsKey(RAccount.KEY_SERVER_LABEL)) {
            return properties[RAccount.KEY_SERVER_LABEL] as String
        }
        return null
    }

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
