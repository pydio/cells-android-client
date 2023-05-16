package com.pydio.android.cells.db.accounts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(account: RAccount)

    @Update
    fun update(account: RAccount)

    @Query("DELETE FROM accounts WHERE account_id = :accountID")
    fun forgetAccount(accountID: String)

    @Query("SELECT * FROM accounts WHERE account_id = :accountID LIMIT 1")
    fun getAccount(accountID: String): RAccount?

    @Query("SELECT * FROM accounts WHERE username = :username AND url = :url LIMIT 1")
    fun getAccount(username: String, url: String): RAccount?

    @Query("SELECT * FROM accounts WHERE url = :url ")
    fun getAccountByUrl(url: String): List<RAccount>

    @Query("SELECT * FROM accounts")
    fun getAccounts(): List<RAccount>
}
