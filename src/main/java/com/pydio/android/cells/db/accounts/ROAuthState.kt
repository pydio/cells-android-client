package com.pydio.android.cells.db.accounts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pydio.cells.api.ServerURL
import com.pydio.android.cells.db.Converters

/**
 *  Stores a map between the state that are generated during the OAuth process
 * and the corresponding {@code ServerURL}
 */
@Entity(tableName = "oauth_states")
@TypeConverters(Converters::class)
data class ROAuthState(

    @PrimaryKey
    @ColumnInfo(name = "oauth_state") val state: String,

    @ColumnInfo(name = "server_url") val serverURL: ServerURL,

    @ColumnInfo(name = "start_ts") val startTimestamp: Long,

    @ColumnInfo(name = "next") val next: String?,

    )