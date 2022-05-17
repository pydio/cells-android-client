package com.pydio.android.cells.db.auth

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LegacyCredentialsDao {

    @Insert
    fun insert(credentials: RLegacyCredentials)

    @Update
    fun update(credentials: RLegacyCredentials)

    @Delete
    fun delete(credentials: RLegacyCredentials)

    @Query("DELETE FROM legacy_credentials WHERE account_id = :accountID")
    fun forgetPassword(accountID: String)

    @Query("SELECT * FROM legacy_credentials WHERE account_id = :accountID LIMIT 1")
    fun getCredential(accountID: String): RLegacyCredentials?

    @Query("SELECT * FROM legacy_credentials")
    fun getAll(): List<RLegacyCredentials>

}