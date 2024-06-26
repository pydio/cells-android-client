package com.pydio.android.cells.db.nodes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
    // FIXME
//     exportSchema = true,
    exportSchema = false,
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
                )
                    // Old dev version of the DB that has made to the Play store during RC phase
                    .fallbackToDestructiveMigrationFrom(1)
                    // Adding a column in the view necessitates a migration
                    // That we do not play anymore (see below)
                    // .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigrationFrom(2)
                    // Making a column String nullable also necessitates a migration
                    // that is unnecessary to implement at this time.
                    .fallbackToDestructiveMigrationFrom(3)
                    // .addMigrations(MIGRATION_3_4)
                    // Ease downgrade for dev purposes, this should not happen in prod
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addMigrations(MIGRATION_4_5)
                    .build()
                INSTANCES[accountId] = instance
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

        // Migrations

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add a column to the transfers table
                db.execSQL("ALTER TABLE transfers ADD COLUMN external_id INTEGER")
            }
        }
    }
}
