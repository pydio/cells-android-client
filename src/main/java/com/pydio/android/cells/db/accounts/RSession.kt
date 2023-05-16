package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pydio.android.cells.AppNames
import com.pydio.cells.transport.StateID
import java.net.URL

@Entity(tableName = "sessions")
data class RSession(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    // foreground, background, paused, new
    @ColumnInfo(name = "lifecycle_state") var lifecycleState: String,

    @ColumnInfo(name = "dir_name") val dirName: String,

    @ColumnInfo(name = "db_name") val dbName: String,

    // We duplicate this info to ease implementation: we can do this because we won't have tons of accounts.
    @ColumnInfo(name = "is_legacy") val isRemoteLegacy: Boolean,

    // Simple flag to make the UI more explicit for the end user.
    // This flag is set to false when we have internet but cannot ping the server.
    // It should not be used to prevent trying to reach the remote server: we should try to ping
    // at each iteration, even when set to false.
    @ColumnInfo(name = "is_reachable", defaultValue = "true") var isReachable: Boolean,
) {

    companion object {

        fun newInstance(account: RAccount, index: Int): RSession {
            var cleanUrl = URL(account.url).host
            var cleanDbName = "nodes.$cleanUrl"

            if (index > 0) {
                cleanUrl += "-$index"
                cleanDbName += "-$index"
            }

            return RSession(
                accountID = account.accountId,
                lifecycleState = AppNames.SESSION_STATE_NEW,
                dirName = cleanUrl,
                dbName = cleanDbName,
                isRemoteLegacy = account.isLegacy,
                isReachable = true
            )
        }
    }

    fun account(): StateID = StateID.fromId(accountID)
}
