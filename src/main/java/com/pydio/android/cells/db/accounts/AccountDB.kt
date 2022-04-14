package com.pydio.android.cells.db.accounts

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = arrayOf(
        RAccount::class,
        RSession::class,
        RWorkspace::class,
        ROAuthState::class,
        RToken::class,
        RLegacyCredentials::class
    ),
    views = arrayOf(RLiveSession::class),
    version = 1,
    exportSchema = false,
)
abstract class AccountDB : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun authStateDao(): OAuthStateDao

    abstract fun legacyCredentialsDao(): LegacyCredentialsDao

    abstract fun liveSessionDao(): LiveSessionDao

    abstract fun sessionDao(): SessionDao

    abstract fun tokenDao(): TokenDao

    abstract fun workspaceDao(): WorkspaceDao

//    companion object {
//        @Volatile
//        private var INSTANCE: AccountDB? = null
//
//        fun getDatabase(context: Context): AccountDB {
//            val tempInstance = INSTANCE
//            if (tempInstance != null) {
//                return tempInstance
//            }
//            synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    AccountDB::class.java,
//                    "accountdb"
//                ).build()
//                INSTANCE = instance
//                return instance
//            }
//        }
//    }
}
