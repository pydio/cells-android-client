package com.pydio.android.cells.db.auth

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RToken::class,
        RLegacyCredentials::class,
        ROAuthState::class,
    ],
    version = 2,
    exportSchema = true,
)

abstract class AuthDB : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE oauth_states ADD COLUMN code_verifier TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    abstract fun tokenDao(): TokenDao

    abstract fun legacyCredentialsDao(): LegacyCredentialsDao

    abstract fun authStateDao(): OAuthStateDao

}
