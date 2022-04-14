package com.pydio.android.cells.db.nodes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface LiveOfflineRootDao {
    @Query("SELECT * FROM RLiveOfflineRoot")
    fun getLiveOfflineRoots(): LiveData<List<RLiveOfflineRoot>>

    @Query("SELECT * FROM RLiveOfflineRoot WHERE uuid = :uuid LIMIT 1")
    fun getByUuid(uuid: String): RLiveOfflineRoot?

    @Query("SELECT * FROM RLiveOfflineRoot ORDER BY sort_name")
    fun getAll(): List<RLiveOfflineRoot>
}