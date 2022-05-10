package com.pydio.android.cells.db.runtime

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(RNetworkInfo::class), version = 1, exportSchema = false)
abstract class RuntimeDB : RoomDatabase() {

    abstract fun networkInfoDao(): NetworkInfoDao
}
