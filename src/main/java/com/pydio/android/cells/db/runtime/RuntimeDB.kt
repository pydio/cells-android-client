package com.pydio.android.cells.db.runtime

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RNetworkInfo::class,
        RJob::class,
        RLog::class
    ],
    version = 1,
    exportSchema = false
)

abstract class RuntimeDB : RoomDatabase() {

    abstract fun networkInfoDao(): NetworkInfoDao

    abstract fun jobDao(): JobDao

    abstract fun logDao(): LogDao
}
