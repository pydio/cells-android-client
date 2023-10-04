package com.pydio.android.legacy.v2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.utils.showMessage
import com.pydio.cells.api.SdkNames
import com.pydio.cells.client.security.LegacyPasswordManager
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.utils.Str
import java.nio.charset.Charset
import java.security.GeneralSecurityException

/**
 * Legacy code, only imported to manage the migration from v2 instances.
 *
 *
 * This code will be deleted very soon.
 */
class V2MainDB private constructor(context: Context, path: String, version: Int) :
    SQLiteOpenHelper(context, path, null, version) {

    private val logTag = "V2MainDB"

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            showMessage(
                CellsApp.instance.applicationContext,
                "Trying to downgrade V2MainDB from $oldVersion to $newVersion"
            )
        } catch (e: Exception) {
            Log.e(logTag, "could not show downgrade issue message, aborting...")
            Log.e(logTag, "    versions values: old: $oldVersion, new: $newVersion")
            e.printStackTrace()
        }
        super.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createSessions)
        db.execSQL(createTokens)
        db.execSQL(create_cookies)
        db.execSQL(create_offline_roots)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(createSessions)
        db.execSQL(createTokens)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    // DAOs
    fun listAccountRecords(): List<AccountRecord> {
        val accountRecords: MutableList<AccountRecord> = ArrayList()
        synchronized(lock) {
            val db = readDB()
            val cursor = db.rawQuery(ALL_ACCOUNTS, null)
            while (cursor.moveToNext()) {
                val blob = cursor.getBlob(1)
                var json = String(blob, Charset.defaultCharset())
                // Also support older legacy DB model
                if (json.contains("mHost")) {
                    json = json.replace("mHost", "host")
                        .replace("mScheme", "scheme")
                        .replace("mPort", "port")
                        .replace("mPath", "path")
                        .replace("mVersion", "version")
                        .replace("mVersionName", "versionName")
                        .replace("mIconURL", "iconURL")
                        .replace("mWelcomeMessage", "welcomeMessage")
                        .replace("mLabel", "label")
                        .replace("mUrl", "url")
                        .replace("mSSLContext", "sslContext")
                        .replace("mSSLUnverified", "sslUnverified")
                        .replace("mLegacy", "legacy")
                        .replace("mProperties", "properties")
                }
                try {
                    val gson = Gson()
                    val record = gson.fromJson(json, AccountRecord::class.java)
                    if (Str.notEmpty(record.accountID)) {
                        accountRecords.add(record)
                    } else {
                        println(" No account ID for current row, skipping")
                    }
                } catch (e: Exception) {
                    println(" Unable to deserialize: " + e.message)
                    e.printStackTrace()
                }
            }
            cursor.close()
        }
        return accountRecords
    }

    fun listLegacyAccountRecords(): List<LegacyAccountRecord> {
        val accountRecords: MutableList<LegacyAccountRecord> = ArrayList()
        synchronized(lock) {
            val db = readDB()
            val cursor = db.rawQuery(ALL_ACCOUNTS, null)
            while (cursor.moveToNext()) {
                val blob = cursor.getBlob(1)
                var json = String(blob, Charset.defaultCharset())
                if (json.contains("mHost")) {
                    json = json.replace("mHost", "host")
                        .replace("mScheme", "scheme")
                        .replace("mPort", "port")
                        .replace("mPath", "path")
                        .replace("mVersion", "version")
                        .replace("mVersionName", "versionName")
                        .replace("mIconURL", "iconURL")
                        .replace("mWelcomeMessage", "welcomeMessage")
                        .replace("mLabel", "label")
                        .replace("mUrl", "url")
                        .replace("mSSLContext", "sslContext")
                        .replace("mSSLUnverified", "sslUnverified")
                        .replace("mLegacy", "legacy")
                        .replace("mProperties", "properties")
                }
                val record = Gson().fromJson(json, LegacyAccountRecord::class.java)
                accountRecords.add(record)
            }
            cursor.close()
        }
        return accountRecords
    }

    fun listAllTokens(): Map<String, Token> {
        val tokens: MutableMap<String, Token> = HashMap()
        synchronized(lock) {
            val db = readDB()
            db.rawQuery(ALL_TOKENS, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val key = cursor.getString(0)
                    val serialized = cursor.getString(1)
                    tokens[key] = Token.decode(serialized)
                }
                return tokens
            }
        }
    }

    fun getToken(key: String): Token? {
        synchronized(lock) {
            val db = readDB()
            db.rawQuery(TOKEN_BY_ID, arrayOf(key)).use { c ->
                if (!c.moveToNext()) {
                    c.close()
                    return null
                }
                val serialized = c.getString(0)
                return Token.decode(serialized)
            }
        }
    }

    fun listAllLegacyPasswords(): Map<String, String> {
        val credentials: MutableMap<String, String> = HashMap()
        synchronized(lock) {
            val db = readDB()
            db.rawQuery(ALL_PASSWORDS, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val user = cursor.getString(0)
                    val pwd = cursor.getString(1)
                    credentials[user] = pwd
                }
            }
        }
        return credentials
    }

    fun getPassword(key: String): String? {
        synchronized(lock) {
            val db = readDB()
            db.rawQuery(PASSWORD_BY_ID, arrayOf(key)).use { c ->
                if (!c.moveToNext()) {
                    c.close()
                    return null
                }
                val password = c.getString(0) ?: return null
                return if (!password.startsWith("\$AJXP_ENC$")) {
                    password
                } else try {
                    LegacyPasswordManager.decrypt(password)
                } catch (e: GeneralSecurityException) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    companion object {
        private const val VERSION = 1
        const val DB_FILE_NAME = "database.sqlite"
        const val DB_FILE_PATH = "/files/$DB_FILE_NAME"
        private val lock = Any()
        private var instance: V2MainDB? = null

        // SQL Boiler plate
        const val sessions = "`sessions`"
        private const val col_sid = "`session_id`"
        private const val col_blob = "`content`"
        private val createSessions = String.format(
            "CREATE TABLE if not exists %s (%s text not null, %s blob not null);",
            sessions,
            col_sid,
            col_blob
        )
        private const val tokens = "`tokens`"
        private const val col_jwt = "`jwt`"
        private val createTokens = String.format(
            "CREATE TABLE if not exists %s (%s text not null, %s text not null);",
            tokens,
            col_sid,
            col_jwt
        )
        private const val col_user = "`user`"
        private const val col_password = "`password`"
        private val create_cookies = String.format(
            "CREATE TABLE if not exists cookies (%s TEXT PRIMARY KEY NOT NULL, %s TEXT); ",
            col_user,
            col_password
        )
        const val index = "`index_group`"
        private const val col_workspace = "`workspace_id`"
        private const val col_path = "`path`"
        private const val col_change_seq = "`" + SdkNames.CHANGE_SEQ + "`"
        private const val create_offline_roots = "CREATE TABLE " + index + "(" +
                col_workspace + " TEXT NOT NULL, " +
                col_path + " TEXT NOT NULL, " +
                col_change_seq + " INTEGER DEFAULT 0, " +
                "UNIQUE (" + col_workspace + ", " + col_path + ")" +
                ");"
        private const val ALL_ACCOUNTS = "select session_id, content from sessions;"
        private const val ALL_TOKENS = "select session_id, jwt from tokens;"
        private const val TOKEN_BY_ID = "select jwt from tokens where session_id=?;"
        private const val ALL_PASSWORDS = "select user, password from cookies;"
        private const val PASSWORD_BY_ID = "select password from cookies where user=?;"

        // DB Lifecycle
        @JvmStatic
        fun init(context: Context, absPath: String) {
            if (instance == null) {
                instance = V2MainDB(context.applicationContext, absPath, VERSION)
            }
        }

        @JvmStatic
        val helper: V2MainDB?
            get() {
                if (instance == null) {
                    throw RuntimeException("you must first call init to inject a context")
                }
                return instance
            }

        private fun readDB(): SQLiteDatabase {
            return helper!!.readableDatabase
        }
    }
}