package com.pydio.android.legacy.v2

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.pydio.android.legacy.v2.V2SyncDB
import com.pydio.android.legacy.v2.WatchInfo
import com.pydio.cells.api.ui.FileNode
import com.google.gson.Gson
import java.lang.RuntimeException
import java.util.ArrayList

class V2SyncDB private constructor(context: Context, filepath: String, version: Int) :
    SQLiteOpenHelper(context, filepath, null, version) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "create table if not exists watched (" +
                    "session_id varchar(255) not null," +
                    "workspace_slug varchar(255) not null," +
                    "path text not null," +
                    "workspace_label varchar(255)," +
                    "encoded text not null," +
                    "add_time int," +
                    "last_sync_time int," +
                    "active int(1) default 1);"
        )
        db.execSQL(
            "create table if not exists stats(" +
                    "session_id varchar(255) not null," +
                    "workspace_slug varchar(255) not null," +
                    "path text not null," +
                    "encoded_stats text" +
                    ");"
        )
        db.execSQL(
            "create table if not exists errors (" +
                    "session_id varchar(255) not null," +
                    "workspace_slug varchar(255) not null," +
                    "path text not null," +
                    "action_code integer not null," +
                    "encoded_error text" +
                    ");"
        )
    }

    /* V1
    public void onCreateV1(SQLiteDatabase db) {
        db.execSQL("create table if not exists watched (" +
                "session_id varchar(255) not null," +
                "workspace_slug varchar(255) not null," +
                "path text not null," +
                "workspace_label varchar(255)," +
                "encoded text not null," +
                "add_time int," +
                "last_sync_time int," +
                "active int(1) default 1);");

        db.execSQL("create table if not exists errors (" +
                "session_id varchar(255) not null," +
                "workspace_slug varchar(255) not null," +
                "path text not null," +
                "action_code integer not null," +
                "time integer" +
                ");");
    }
    */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            Log.i(logTag, "Running migration to version 2")
            db.execSQL(
                "create table if not exists stats(" +
                        "session_id varchar(255) not null," +
                        "workspace_slug varchar(255) not null," +
                        "path text not null," +
                        "encoded_stats text" +
                        ");"
            )
            db.execSQL("drop table errors;")
            db.execSQL(
                "create table errors (" +
                        "session_id varchar(255) not null," +
                        "workspace_slug varchar(255) not null," +
                        "path text not null," +
                        "action_code integer not null," +
                        "encoded_error text" +
                        ");"
            )
        }
    }

    val all: List<WatchInfo>
        get() {
            val list: MutableList<WatchInfo> = ArrayList()
            val db = helper!!.readableDatabase
            db.rawQuery(
                "select session_id, workspace_label, add_time, last_sync_time, encoded, active from watched where active=1;",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val info = WatchInfo()
                    info.accountID = c.getString(0)
                    info.workspaceLabel = c.getString(1)
                    info.addTime = c.getLong(2)
                    info.lastSyncTime = c.getLong(3)
                    val encoded = c.getString(4)
                    info.node = gson.fromJson(encoded, FileNode::class.java)
                    info.isActive = c.getInt(5) == 1
                    list.add(info)
                }
            }
            return list
        }

    fun getWatches(accountID: String): List<WatchInfo> {
        val list: MutableList<WatchInfo> = ArrayList()
        val db = helper!!.readableDatabase
        db.rawQuery(
            "select " +
                    "session_id, " +
                    "workspace_label, " +
                    "add_time, " +
                    "last_sync_time, " +
                    "encoded, " +
                    "active " +
                    "from watched where " +
                    "session_id=?;", arrayOf(accountID)
        ).use { c ->
            while (c.moveToNext()) {
                val info = WatchInfo()
                info.accountID = c.getString(0)
                info.workspaceLabel = c.getString(1)
                info.addTime = c.getLong(2)
                info.lastSyncTime = c.getLong(3)
                val encoded = c.getString(4)
                info.node = gson.fromJson(encoded, FileNode::class.java)
                info.isActive = c.getInt(5) == 1
                list.add(info)
            }
        }
        return list
    }

    companion object {
        private val logTag = V2SyncDB::class.java.simpleName
        private const val version = 2
        const val DB_FILE_NAME = "sync.sqlite"
        const val DB_FILE_PATH = "/files/" + DB_FILE_NAME
        private var instance: V2SyncDB? = null
        private val gson = Gson()
        @JvmStatic
        fun init(context: Context, absPath: String) {
            if (instance == null) {
                instance = V2SyncDB(context, absPath, version)
            }
        }

        @JvmStatic
        val helper: V2SyncDB?
            get() {
                if (instance == null) {
                    throw RuntimeException("you must first call init to inject a context")
                }
                return instance
            }
    }
}