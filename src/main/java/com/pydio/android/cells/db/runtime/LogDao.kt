package com.pydio.android.cells.db.runtime

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {

    @Insert
    fun insert(log: RLog): Long

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getLiveLogs(): LiveData<List<RLog>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 100")
    fun getLatest(): List<RLog>

    @Query("SELECT * FROM logs WHERE level <= :levelId ORDER BY timestamp DESC LIMIT 100")
    fun getByLevelAtLeast(levelId: Int): List<RLog>

    @Query("SELECT * FROM logs WHERE level = :levelId ORDER BY timestamp DESC LIMIT 100")
    fun getByLevel(levelId: Int): List<RLog>

}
