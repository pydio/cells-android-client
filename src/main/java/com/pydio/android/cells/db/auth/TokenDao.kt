package com.pydio.android.cells.db.auth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TokenDao {

    @Insert
    fun insert(RToken: RToken)

    @Update
    fun update(RToken: RToken)

    @Query("SELECT * FROM tokens WHERE account_id = :accountID LIMIT 1")
    fun getToken(accountID: String): RToken?

    @Query("DELETE FROM tokens WHERE account_id = :accountID")
    fun deleteToken(accountID: String)

    @Query("DELETE FROM tokens")
    fun deleteAllToken()

    @Query("SELECT * FROM tokens")
    fun getAll(): List<RToken>
}
