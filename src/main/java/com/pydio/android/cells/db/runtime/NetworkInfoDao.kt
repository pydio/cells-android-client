package com.pydio.android.cells.db.runtime

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NetworkInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(networkInfo: RNetworkInfo): Long

    @Update
    fun update(networkInfo: RNetworkInfo)

    @Query("SELECT * FROM network_info LIMIT 1")
    fun get(): RNetworkInfo?

    @Query("SELECT * FROM network_info LIMIT 1")
    fun getLive(): LiveData<RNetworkInfo>

}