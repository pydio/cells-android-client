package com.pydio.android.cells.legacy.v2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.pydio.cells.api.ui.FileNode;

import java.util.ArrayList;
import java.util.List;

public class SyncDB extends SQLiteOpenHelper {

    private final static String logTag = SyncDB.class.getSimpleName();
    private static final int version = 2;
    public final static String DB_FILE_PATH = "/files/sync.sqlite";

    private static SyncDB instance;

    private final static Gson gson = new Gson();

    public static void init(Context context, String absPath) {
        if (instance == null) {
            instance = new SyncDB(context, absPath, version);
        }
    }

    public static SyncDB getHelper() {
        if (instance == null) {
            throw new RuntimeException("you must first call init to inject a context");
        }
        return instance;
    }

    private SyncDB(Context context, String filepath, int version) {
        super(context, filepath, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table if not exists watched (" +
                "session_id varchar(255) not null," +
                "workspace_slug varchar(255) not null," +
                "path text not null," +
                "workspace_label varchar(255)," +
                "encoded text not null," +
                "add_time int," +
                "last_sync_time int," +
                "active int(1) default 1);");

        db.execSQL("create table if not exists stats(" +
                "session_id varchar(255) not null," +
                "workspace_slug varchar(255) not null," +
                "path text not null," +
                "encoded_stats text" +
                ");");

        db.execSQL("create table if not exists errors (" +
                "session_id varchar(255) not null," +
                "workspace_slug varchar(255) not null," +
                "path text not null," +
                "action_code integer not null," +
                "encoded_error text" +
                ");");
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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion == 1) {
            Log.i(logTag, "Running migration to version 2");
            db.execSQL("create table if not exists stats(" +
                    "session_id varchar(255) not null," +
                    "workspace_slug varchar(255) not null," +
                    "path text not null," +
                    "encoded_stats text" +
                    ");");

            db.execSQL("drop table errors;");
            db.execSQL("create table errors (" +
                    "session_id varchar(255) not null," +
                    "workspace_slug varchar(255) not null," +
                    "path text not null," +
                    "action_code integer not null," +
                    "encoded_error text" +
                    ");");
        }
    }

    public List<WatchInfo> getAll() {
        List<WatchInfo> list = new ArrayList<>();
        SQLiteDatabase db = getHelper().getReadableDatabase();
        try (Cursor c = db.rawQuery("select session_id, workspace_label, add_time, last_sync_time, encoded, active from watched where active=1;", null)) {
            while (c.moveToNext()) {
                WatchInfo info = new WatchInfo();
                info.setAccountID(c.getString(0));
                info.setWorkspaceLabel(c.getString(1));
                info.setAddTime(c.getLong(2));
                info.setLastSyncTime(c.getLong(3));
                String encoded = c.getString(4);

                info.setNode(gson.fromJson(encoded, FileNode.class));
                info.setActive(c.getInt(5) == 1);
                list.add(info);
            }
        }
        return list;
    }

    public List<WatchInfo> getWatches(String accountID) {
        List<WatchInfo> list = new ArrayList<>();
        SQLiteDatabase db = getHelper().getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "select " +
                        "session_id, " +
                        "workspace_label, " +
                        "add_time, " +
                        "last_sync_time, " +
                        "encoded, " +
                        "active " +
                        "from watched where " +
                        "session_id=?;", new String[]{accountID})) {

            while (c.moveToNext()) {
                WatchInfo info = new WatchInfo();
                info.setAccountID(c.getString(0));
                info.setWorkspaceLabel(c.getString(1));
                info.setAddTime(c.getLong(2));
                info.setLastSyncTime(c.getLong(3));
                String encoded = c.getString(4);

                info.setNode(gson.fromJson(encoded, FileNode.class));
                info.setActive(c.getInt(5) == 1);
                list.add(info);
            }
        }
        return list;
    }
}