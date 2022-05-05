package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.cells.api.Server
import com.pydio.cells.transport.StateID
import com.pydio.android.cells.AppNames

@Entity(tableName = "accounts")
data class RAccount(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    @ColumnInfo(name = "url") val url: String,

    @ColumnInfo(name = "username") val username: String,

    @ColumnInfo(name = "auth_status") var authStatus: String,

    // 0 = normal, 1 = skip verify, 2 = custom certificate (to be implemented)
    @ColumnInfo(name = "tls_mode") var tlsMode: Int = 0,

    @ColumnInfo(name = "is_legacy") var isLegacy: Boolean = false,

    // TODO Rather use properties to simply be able to enrich model?
    @ColumnInfo(name = "server_label") val serverLabel: String?,

    @ColumnInfo(name = "welcome_message") val welcomeMessage: String?,
)

fun toRAccount(username: String, server: Server): RAccount {
    return RAccount(
        accountID = StateID(username, server.url()).accountId,
        username = username,
        url = server.url(),
        serverLabel = server.label,
        tlsMode = if (server.serverURL.skipVerify()) 1 else 0,
        isLegacy = server.isLegacy,
        welcomeMessage = server.welcomeMessage,
        authStatus = AppNames.AUTH_STATUS_NEW,
    )
}
