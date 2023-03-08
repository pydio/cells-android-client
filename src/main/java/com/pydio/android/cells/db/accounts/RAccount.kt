package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.Converters
import com.pydio.cells.api.Server
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.util.*

@Entity(tableName = "accounts")
@TypeConverters(Converters::class)
data class RAccount(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    @ColumnInfo(name = "url") val url: String,

    @ColumnInfo(name = "username") val username: String,

    @ColumnInfo(name = "auth_status") var authStatus: String,

    // 0 = normal, 1 = skip verify, 2 = custom certificate (to be implemented)
    @ColumnInfo(name = "tls_mode") var tlsMode: Int = 0,

    @ColumnInfo(name = "is_legacy") var isLegacy: Boolean = false,

    // We rather use properties to simply be able to enrich model
    @ColumnInfo(name = "properties") val properties: Properties,
//    @ColumnInfo(name = "server_label") val serverLabel: String?,
//    @ColumnInfo(name = "welcome_message") val welcomeMessage: String?,
) {

    companion object {
        // private val logTag = "RAccount"

        const val KEY_SERVER_LABEL = "server_label"
        const val KEY_WELCOME_MESSAGE = "welcome_message"

        fun toRAccount(username: String, server: Server): RAccount {

            val props = Properties()
            if (Str.notEmpty(server.label)) {
                props.setProperty(KEY_SERVER_LABEL, server.label)
            }
            if (Str.notEmpty(server.welcomeMessage)) {
                props.setProperty(KEY_WELCOME_MESSAGE, server.welcomeMessage)
            }

            return RAccount(
                accountID = StateID(username, server.url()).accountId,
                username = username,
                url = server.url(),
                tlsMode = if (server.serverURL.skipVerify()) 1 else 0,
                isLegacy = server.isLegacy,
                authStatus = AppNames.AUTH_STATUS_NEW,
                properties = props,
            )
        }
    }

    fun account(): StateID = StateID.fromId(accountID)

    fun skipVerify() = tlsMode != 0

    fun serverLabel(): String? {
        if (properties.containsKey(KEY_SERVER_LABEL)) {
            return properties[KEY_SERVER_LABEL] as String
        }
        return null
    }

    fun welcomeMessage(): String? {
        if (properties.containsKey(KEY_WELCOME_MESSAGE)) {
            return properties[KEY_WELCOME_MESSAGE] as String
        }
        return null
    }
}
