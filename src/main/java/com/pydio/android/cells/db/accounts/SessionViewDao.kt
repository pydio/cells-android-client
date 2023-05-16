package com.pydio.android.cells.db.accounts

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionViewDao {

    // Suspend functions to be called from the Io dispatcher
    @Query("SELECT * FROM RSessionView where account_id = :accountID")
    fun getSession(accountID: String): RSessionView?

    @Query("SELECT * FROM RSessionView where lifecycle_state = :state LIMIT 1")
    fun getActiveSession(state: String): RSessionView?

    @Query("SELECT * FROM RSessionView")
    fun getSessions(): List<RSessionView>

    @Query("SELECT * FROM RSessionView where is_legacy = 0")
    fun getCellsSessions(): List<RSessionView>

    // Exposes Reactive Flows

    @Query("SELECT * FROM RSessionView")
    fun getLiveSessions(): Flow<List<RSessionView>>

    @Query("SELECT * FROM RSessionView where account_id = :accountID")
    fun getSessionFlow(accountID: String): Flow<RSessionView?>

    @Query("SELECT * FROM RSessionView where lifecycle_state = :state LIMIT 1")
    fun getActiveSessionFlow(state: String): Flow<RSessionView?>
}
