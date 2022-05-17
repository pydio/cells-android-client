package com.pydio.android.cells.db.accounts

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface SessionViewDao {

    @Query("SELECT * FROM RSessionView")
    fun getLiveSessions(): LiveData<List<RSessionView>>

    @Query("SELECT * FROM RSessionView")
    fun getSessions(): List<RSessionView>

    @Query("SELECT * FROM RSessionView where is_legacy = 0")
    fun getCellsSessions(): List<RSessionView>

    @Query("SELECT * FROM RSessionView where account_id = :accountID")
    fun getLiveSession(accountID: String): LiveData<RSessionView?>

    @Query("SELECT * FROM RSessionView where account_id = :accountID")
    fun getSession(accountID: String): RSessionView?

    @Query("SELECT * FROM RSessionView where lifecycle_state = :state LIMIT 1")
    fun getLiveActiveSession(state: String): LiveData<RSessionView?>

}
