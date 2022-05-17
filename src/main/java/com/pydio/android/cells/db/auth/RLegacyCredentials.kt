package com.pydio.android.cells.db.auth

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "legacy_credentials")
data class RLegacyCredentials(

    @PrimaryKey
    @ColumnInfo(name = "account_id") val accountID: String,

    @ColumnInfo(name = "password") val password: String,
)
