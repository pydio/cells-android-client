package com.pydio.android.cells.db.accounts

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RAccount::class,
        RSession::class,
        RWorkspace::class,
    ],
    views = [RSessionView::class],
    version = 2,
// FIXME
//     exportSchema = true,
    exportSchema = false,
//    autoMigrations = [
//        AutoMigration(from = 1, to = 2)
//    ],
)
abstract class AccountDB : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun sessionDao(): SessionDao

    abstract fun sessionViewDao(): SessionViewDao

    abstract fun workspaceDao(): WorkspaceDao

}
