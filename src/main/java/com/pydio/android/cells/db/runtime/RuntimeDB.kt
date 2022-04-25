package com.pydio.android.cells.db.runtime

class RuntimeDB

/*
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.TransferDao

@Database(entities = arrayOf(RTransfer::class), version = 1, exportSchema = false)
abstract class RuntimeDB : RoomDatabase() {


    companion object {
        @Volatile
        private var INSTANCE: RuntimeDB? = null

        fun getDatabase(context: Context): RuntimeDB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RuntimeDB::class.java,
                    "runtime_objects"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
*/
