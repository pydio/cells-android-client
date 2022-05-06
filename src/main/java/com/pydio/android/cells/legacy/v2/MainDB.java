package com.pydio.android.cells.legacy.v2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.pydio.cells.api.SdkNames;
import com.pydio.cells.client.security.LegacyPasswordManager;
import com.pydio.cells.transport.auth.Token;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legacy code, only imported to manage the migration from v2 instances.
 * <p>
 * This code will be deleted very soon.
 */
public class MainDB extends SQLiteOpenHelper {

    public final static int VERSION = 1;
    public final static String DB_FILE_PATH = "/files/database.sqlite";

    private static final Object lock = new Object();
    private static MainDB instance;

    // SQL Boiler plate
    public static final String sessions = "`sessions`";
    public static final String col_sid = "`session_id`";
    public static final String col_blob = "`content`";
    private final static String createSessions = String.format(
            "CREATE TABLE if not exists %s (%s text not null, %s blob not null);", sessions, col_sid, col_blob);

    public static final String tokens = "`tokens`";
    public static final String col_jwt = "`jwt`";
    private final static String createTokens = String.format(
            "CREATE TABLE if not exists %s (%s text not null, %s text not null);", tokens, col_sid, col_jwt);

    public static final String col_user = "`user`";
    public static final String col_password = "`password`";
    private static final String create_cookies = String.format(
            "CREATE TABLE if not exists cookies (%s TEXT PRIMARY KEY NOT NULL, %s TEXT); ", col_user, col_password);

    public static final String index = "`index_group`";
    public static final String col_workspace = "`workspace_id`";
    public static final String col_path = "`path`";
    public static final String col_change_seq = "`" + SdkNames.CHANGE_SEQ + "`";
    private final static String create_offline_roots =
            "CREATE TABLE " + index + "(" +
                    col_workspace + " TEXT NOT NULL, " +
                    col_path + " TEXT NOT NULL, " +
                    col_change_seq + " INTEGER DEFAULT 0, " +
                    "UNIQUE (" + col_workspace + ", " + col_path + ")" +
                    ");";

    private final static String ALL_ACCOUNTS = "select session_id, content from sessions;";
    private final static String ALL_TOKENS = "select session_id, jwt from tokens;";
    private final static String TOKEN_BY_ID = "select jwt from tokens where session_id=?;";
    private final static String ALL_PASSWORDS = "select user, password from cookies;";
    private final static String PASSWORD_BY_ID = "select password from cookies where user=?;";

    // DB Lifecycle
    public static void init(Context context, String absPath) {
        if (instance == null) {
            instance = new MainDB(context.getApplicationContext(), absPath, VERSION);
        }
    }

    public static MainDB getHelper() {
        if (instance == null) {
            throw new RuntimeException("you must first call init to inject a context");
        }
        return instance;
    }

    private MainDB(Context context, String path, int version) {
        super(context, path, null, version);
    }

    private static SQLiteDatabase readDB() {
        return getHelper().getReadableDatabase();
    }
    // private static SQLiteDatabase writeDB() { return getHelper().getWritableDatabase(); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createSessions);
        db.execSQL(createTokens);
        db.execSQL(create_cookies);
        db.execSQL(create_offline_roots);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(createSessions);
        db.execSQL(createTokens);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    // DAOs

    public List<AccountRecord> listAccountRecords() {

        List<AccountRecord> sessions = new ArrayList<>();
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            Cursor cursor = db.rawQuery(ALL_ACCOUNTS, null);
            while (cursor.moveToNext()) {
                byte[] blob = cursor.getBlob(1);
                String json = new String(blob, Charset.defaultCharset());
                try {
                    AccountRecord s = new Gson().fromJson(json, AccountRecord.class);
                    sessions.add(s);
                } catch (Exception e) {
                    System.out.println(" Unable to deserialize: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
        return sessions;
    }

    public static List<LegacyAccountRecord> listLegacyAccountRecords() {
        List<LegacyAccountRecord> sessions = new ArrayList<>();
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            Cursor cursor = db.rawQuery(ALL_ACCOUNTS, null);
            while (cursor.moveToNext()) {
                byte[] blob = cursor.getBlob(1);
                String json = new String(blob, Charset.defaultCharset());
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
                            .replace("mProperties", "properties");
                }
                LegacyAccountRecord s = new Gson().fromJson(json, LegacyAccountRecord.class);
                sessions.add(s);
            }
            cursor.close();
        }
        return sessions;
    }


    public Map<String, Token> listAllTokens() {
        Map<String, Token> tokens = new HashMap<>();
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            try (Cursor cursor = db.rawQuery(ALL_TOKENS, null)) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    String serialized = cursor.getString(1);
                    tokens.put(key, Token.decode(serialized));
                }
                return tokens;
            }
        }
    }

    public Token getToken(String key) {
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            try (Cursor c = db.rawQuery(TOKEN_BY_ID, new String[]{key})) {
                if (!c.moveToNext()) {
                    c.close();
                    return null;
                }
                String serialized = c.getString(0);
                return Token.decode(serialized);
            }
        }
    }

    public Map<String, String> listAllLegacyPasswords() {
        Map<String, String> creds = new HashMap<>();
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            try (Cursor cursor = db.rawQuery(ALL_PASSWORDS, null)) {
                while (cursor.moveToNext()) {
                    String user = cursor.getString(0);
                    String pwd = cursor.getString(1);
                    creds.put(user, pwd);
                }
            }
        }
        return creds;
    }

    public String getPassword(String key) {
        synchronized (lock) {
            SQLiteDatabase db = readDB();
            try (Cursor c = db.rawQuery(PASSWORD_BY_ID, new String[]{key})) {
                if (!c.moveToNext()) {
                    c.close();
                    return null;
                }

                String password = c.getString(0);
                if (password == null) {
                    return null;
                }

                if (!password.startsWith("$AJXP_ENC$")) {
                    return password;
                }

                try {
                    return LegacyPasswordManager.decrypt(password);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    return null;
                }

            }
        }
    }
}
