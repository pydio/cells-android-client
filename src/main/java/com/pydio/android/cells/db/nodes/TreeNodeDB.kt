package com.pydio.android.cells.db.nodes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ConcurrentHashMap

@Database(
    entities = [
        RTreeNode::class,
        RLocalFile::class,
        ROfflineRoot::class,
        RTransfer::class,
        RTransferCancellation::class
    ],
    views = [
        RLiveOfflineRoot::class
    ],
    version = 2,
    exportSchema = true,
)
abstract class TreeNodeDB : RoomDatabase() {

    abstract fun treeNodeDao(): TreeNodeDao

    abstract fun localFileDao(): LocalFileDao

    abstract fun offlineRootDao(): OfflineRootDao

    abstract fun liveOfflineRootDao(): LiveOfflineRootDao

    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCES: ConcurrentHashMap<String, TreeNodeDB> = ConcurrentHashMap()

        fun getDatabase(context: Context, accountId: String, dbName: String): TreeNodeDB {
            val tempInstance = INSTANCES[accountId]
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeNodeDB::class.java,
                    dbName
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCES.put(accountId, instance)
                return instance
            }
        }

        fun closeDatabase(context: Context, accountId: String, dbName: String) {
            val tempInstance = INSTANCES[accountId]
            if (tempInstance != null) {
                synchronized(this) {
                    INSTANCES.remove(accountId)
                }
                tempInstance.close()
                context.deleteDatabase(dbName)
            }
        }
    }
}
