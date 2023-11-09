package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.db.CellsConverters
import com.pydio.cells.api.Server
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.util.*

@Entity(tableName = "accounts")
@TypeConverters(CellsConverters::class)
data class RAccount(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountId: String,

    @ColumnInfo(name = "url") val url: String,

    @ColumnInfo(name = "username") val username: String,

    @ColumnInfo(name = "auth_status") var authStatus: String,

    // 0 = normal, 1 = skip verify, 2 = custom certificate (to be implemented)
    @ColumnInfo(name = "tls_mode") var tlsMode: Int = 0,

    @ColumnInfo(name = "is_legacy") var isLegacy: Boolean = false,

    // We rather use properties to simply be able to enrich model
    @ColumnInfo(name = "properties") val properties: Properties,
) {

    companion object {
        // private val logTag = "RAccount"

        const val KEY_SERVER_LABEL = "server_label"
        const val KEY_WELCOME_MESSAGE = "welcome_message"
        const val KEY_CUSTOM_COLOR = "custom_color"

        fun toRAccount(username: String, server: Server): RAccount {

            val props = Properties()
            if (Str.notEmpty(server.label)) {
                props.setProperty(KEY_SERVER_LABEL, server.label)
            }
            if (Str.notEmpty(server.welcomeMessage)) {
                props.setProperty(KEY_WELCOME_MESSAGE, server.welcomeMessage)
            }
            if (Str.notEmpty(server.customPrimaryColor)) {
                props.setProperty(KEY_CUSTOM_COLOR, server.customPrimaryColor)
            }

            return RAccount(
                accountId = StateID(username, server.url()).accountId,
                username = username,
                url = server.url(),
                tlsMode = if (server.serverURL.skipVerify()) 1 else 0,
                isLegacy = server.isLegacy,
                authStatus = LoginStatus.New.id,
                properties = props,
            )
        }
    }

    fun accountID(): StateID = StateID.fromId(accountId)

    fun skipVerify() = tlsMode != 0

    fun isLoggedIn(): Boolean {
        return authStatus == LoginStatus.Connected.id
    }

    fun serverLabel(): String? {
        if (properties.containsKey(KEY_SERVER_LABEL)) {
            return properties[KEY_SERVER_LABEL] as String
        }
        return null
    }

    fun setLabel(label: String) {
        properties[KEY_SERVER_LABEL] = label
    }

    fun setCustomColor(colorString: String) {
        properties[KEY_CUSTOM_COLOR] = colorString
    }

    fun getCustomColor(): String? {
        if (properties.containsKey(KEY_CUSTOM_COLOR)) {
            return properties[KEY_CUSTOM_COLOR] as String
        }
        return null
    }

    fun setWelcomeMessage(message: String) {
        properties[KEY_WELCOME_MESSAGE] = message
    }

    fun welcomeMessage(): String? {
        if (properties.containsKey(KEY_WELCOME_MESSAGE)) {
            return properties[KEY_WELCOME_MESSAGE] as String
        }
        return null
    }
}
