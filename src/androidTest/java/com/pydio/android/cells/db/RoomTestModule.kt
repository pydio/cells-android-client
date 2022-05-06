package com.pydio.android.cells.db

import androidx.room.Room
import com.pydio.android.cells.db.accounts.AccountDB
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * In-Memory Room MainDB definition
 */
val dbTestModule = module {
    single {
        // In-Memory database config
        Room.inMemoryDatabaseBuilder(androidContext().applicationContext, AccountDB::class.java)
            .allowMainThreadQueries()
            .build()
    }
}