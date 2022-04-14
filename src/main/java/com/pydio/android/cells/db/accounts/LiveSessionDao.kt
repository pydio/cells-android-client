package com.pydio.android.cells.db.accounts

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface LiveSessionDao {

    @Query("SELECT * FROM RLiveSession")
    fun getLiveSessions(): LiveData<List<RLiveSession>>

    @Query("SELECT * FROM RLiveSession")
    fun getSessions(): List<RLiveSession>

    @Query("SELECT * FROM RLiveSession where is_legacy = 'false'")
    fun getCellsSessions(): List<RLiveSession>

    @Query("SELECT * FROM RLiveSession where account_id = :accountID")
    fun getLiveSession(accountID: String): LiveData<RLiveSession?>

    @Query("SELECT * FROM RLiveSession where account_id = :accountID")
    fun getSession(accountID: String): RLiveSession?

    @Query("SELECT * FROM RLiveSession where lifecycle_state = :state LIMIT 1")
    fun getLiveActiveSession(state: String): LiveData<RLiveSession?>

}