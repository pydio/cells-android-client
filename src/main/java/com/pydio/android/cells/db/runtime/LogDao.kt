package com.pydio.android.cells.db.runtime

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {

    @Insert
    fun insert(log: RLog): Long

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 100")
    fun getLatest(levelId: Int): List<RJob>

    @Query("SELECT * FROM logs WHERE level > :levelId ORDER BY timestamp DESC LIMIT 100")
    fun getByLevelAtLeast(levelId: Int): List<RJob>

    @Query("SELECT * FROM logs WHERE level = :levelId ORDER BY timestamp DESC LIMIT 100")
    fun getByLevel(levelId: Int): List<RJob>

}
