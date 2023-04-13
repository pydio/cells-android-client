package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.Converters
import com.pydio.cells.transport.StateID
import java.net.URL

@Entity(tableName = "sessions")
@TypeConverters(Converters::class)
data class RSession(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    // foreground, background, paused, new
    @ColumnInfo(name = "lifecycle_state") var lifecycleState: String,

    @ColumnInfo(name = "dir_name") val dirName: String,

    @ColumnInfo(name = "db_name") val dbName: String,

    // We duplicate this info to ease implementation: we can do this because we won't have tons of accounts.
    @ColumnInfo(name = "is_legacy") val isRemoteLegacy: Boolean,
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
                isRemoteLegacy = account.isLegacy
            )
        }
    }

    fun account(): StateID = StateID.fromId(accountID)
}
