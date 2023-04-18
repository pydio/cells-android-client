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

        // Migrations

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // Add a column to the LiveOfflineRoot view
                database.execSQL("DROP VIEW `RLiveOfflineRoot`")
                val newViewSQL = "CREATE VIEW `RLiveOfflineRoot` " +
                        "AS SELECT offline_roots.encoded_state, offline_roots.uuid, " +
                        "offline_roots.status, offline_roots.local_mod_ts, " +
                        "offline_roots.last_check_ts, offline_roots.message, " +
                        "tree_nodes.mime, tree_nodes.name, tree_nodes.size, " +
                        "tree_nodes.etag, tree_nodes.remote_mod_ts, tree_nodes.flags, " +
                        "offline_roots.sort_name FROM offline_roots " +
                        "INNER JOIN tree_nodes ON offline_roots.encoded_state = tree_nodes.encoded_state"
                database.execSQL(newViewSQL)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add a column to the transfers table
                //                 database.execSQL("ALTER TABLE transfers ADD COLUMN external_id INTEGER DEFAULT -1")
                database.execSQL("ALTER TABLE transfers ADD COLUMN external_id INTEGER")
            }
        }
    }
}
