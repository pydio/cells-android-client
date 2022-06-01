package com.pydio.android.cells.db.nodes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.pydio.android.cells.AppNames

@Dao
interface LiveOfflineRootDao {

    @Query("SELECT * FROM RLiveOfflineRoot WHERE status != :lostStatus ORDER BY sort_name")
    fun getLiveOfflineRoots(lostStatus: String = AppNames.OFFLINE_STATUS_LOST): LiveData<List<RLiveOfflineRoot>>

    @Query("SELECT * FROM RLiveOfflineRoot WHERE uuid = :uuid LIMIT 1")
    fun getByUuid(uuid: String): RLiveOfflineRoot?

    @Query("SELECT * FROM RLiveOfflineRoot ORDER BY sort_name")
    fun getAll(): List<RLiveOfflineRoot>
}