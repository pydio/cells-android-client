package com.pydio.android.cells.db.nodes

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveOfflineRootDao {

    @RawQuery(observedEntities = [RLiveOfflineRoot::class])
    fun offlineRootQueryF(query: SupportSQLiteQuery): Flow<List<RLiveOfflineRoot>>

//    @Query("SELECT * FROM RLiveOfflineRoot WHERE uuid = :uuid LIMIT 1")
//    fun getByUuid(uuid: String): RLiveOfflineRoot?
//
//    @Query("SELECT * FROM RLiveOfflineRoot ORDER BY sort_name")
//    fun getAll(): List<RLiveOfflineRoot>
}
