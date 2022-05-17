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
    version = 1,
    exportSchema = false,
)

abstract class AccountDB : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun sessionDao(): SessionDao

    abstract fun liveSessionDao(): SessionViewDao

    abstract fun workspaceDao(): WorkspaceDao

}
