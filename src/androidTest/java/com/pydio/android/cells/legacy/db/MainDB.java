package com.pydio.android.cells.legacy.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.pydio.android.cells.legacy.db.model.AccountRecord;
import com.pydio.android.cells.legacy.db.model.LegacyAccountRecord;
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

    /* PASSWORDS */

//    public static String[] users() {
//        synchronized (lock) {
//            try (Cursor cursor = MainDB.getHelper().getReadableDatabase().query(cookies, new String[]{col_user}, null, null, null, null, null)) {
//                if (!cursor.moveToFirst()) {
//                    return null;
//                }
//                String[] result = new String[cursor.getCount()];
//                int i = 0;
//                boolean can_browse = true;
//                while (can_browse) {
//                    result[i] = cursor.getString(0);
//                    i++;
//                    can_browse = cursor.moveToNext();
//                }
//                return result;
//            }
//        }
//    }
//
//    public static void addPassword(String user, String password) {
//        synchronized (lock) {
//            android.content.ContentValues values = new android.content.ContentValues();
//            values.put(col_user, user);
//            values.put(col_password, password);
//            SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
//            db.insertWithOnConflict(cookies, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        }
//    }
//
//    public static void updatePassword(String user, String newUser, String newPassword) {
//        synchronized (lock) {
//            android.content.ContentValues values = new android.content.ContentValues();
//            values.put(col_user, newUser);
//            values.put(col_password, newPassword);
//            SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
//            db.update(cookies, values, col_user + "=?", new String[]{user});
//        }
//    }
//
//    public static void deletePassword(String user) {
//        synchronized (lock) {
//            SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
//            db.delete(cookies, String.format("%s='%s'", col_user, user), null);
//        }
//    }
//
//    public static void deletePasswords(String server) {
//        synchronized (lock) {
//            SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
//            db.delete(cookies, String.format("%s='%s'", col_user, "%" + server.replace("://", "+").replace("/", "&")), null);
//            db.close();
//        }
//    }

    /* PROPERTIES */

//    public static String getProperty(String key) {
//        synchronized (lock) {
//            return getProperty(key, null);
//        }
//    }
//
//    public static void setProperty(String name, String value) {
//        synchronized (lock) {
//            android.content.ContentValues values = new android.content.ContentValues();
//            values.put(col_name, name);
//            values.put(col_value, value);
//            SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
//            db.insertWithOnConflict(properties, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        }
//    }
//
//    public static String getProperty(String key, String defaultValue) {
//        synchronized (lock) {
//            try {
//                Cursor cursor = MainDB.getHelper().getReadableDatabase().query(properties, new String[]{col_value}, col_name + "=?", new String[]{key}, null, null, null);
//                if (!cursor.moveToFirst()) {
//                    return null;
//                }
//                String password = cursor.getString(0);
//                cursor.close();
//                return password;
//            } catch (Exception e) {
//                return defaultValue;
//            }
//        }
//    }

    /* SESSIONS */

//    public static boolean saveAccount(AccountRecord s) {
//        synchronized (lock) {
//            SQLiteDatabase db = writeDB();
//            Gson gson = new Gson();
//            String encoded = gson.toJson(s);
//            byte[] bytes = encoded.getBytes(Charset.defaultCharset());
//            db.execSQL(deleteAccountSQL, new String[]{s.id()});
//            db.execSQL(saveAccountSQL, new Object[]{s.id(), bytes});
//            return true;
//        }
//    }
//
//    public static void deleteAccountRecord(String id) {
//        synchronized (lock) {
//            SQLiteDatabase db = writeDB();
//            db.execSQL(deleteAccountSQL, new String[]{id});
//        }
//    }
//
//    public static AccountRecord getAccountRecord(String id) {
//        synchronized (lock) {
//            SQLiteDatabase db = readDB();
//            Cursor c = db.rawQuery(getAccountSQL, new String[]{id});
//            if (!c.moveToNext()) {
//                c.close();
//                return null;
//            }
//
//            byte[] blob = c.getBlob(0);
//            c.close();
//            String json = new String(blob, Charset.defaultCharset());
//            return new Gson().fromJson(json, AccountRecord.class);
//        }
//    }

    /* Tokens */

//    public static void saveToken(String key, Token t) {
//        synchronized (lock) {
//            SQLiteDatabase db = writeDB();
//            db.execSQL(delJWTSQL, new String[]{key});
//            db.execSQL(saveJWTSQL, new Object[]{key, Token.encode(t)});
//        }
//    }
//
//    public static void deleteToken(String key) {
//        synchronized (lock) {
//            SQLiteDatabase db = writeDB();
//            db.execSQL(delJWTSQL, new String[]{key});
//        }
//    }


//    public static final String col_name = "`name`";
//    public static final String col_value = "`value`";
//    public static final String col_alias = "`alias`";
//    public static final String col_certificate = "`certificate`";


    /* SCHEMA */
    //offline tables


//    public static final String offline_tasks = "`offline_tasks`";
//    public static final String offline_events = "`offline_events`";
//    public static final String offline_folders = "`offline_folders`";
//    public static final String session_folders = "`session_folders`";
//    public static final String seqs = "`changes_seq`";
//    public static final String changes = "`changes`";
//    public static final String certificates = "`certificates`";
//    public static final String cookies = "`cookies`";
//    public static final String properties = "`properties`";
//    public static final String transfers = "`transfers`";
//
//
//    public static final String col_transfer_id = "`transfer_id`";
//    public static final String col_type = "`type`";
//    public static final String col_status = "`status`";
//    public static final String col_local = "`local`";
//    public static final String col_remote = "`remote`";
//    public static final String col_size = "`user`";
//
//    public static final String col_session_id = "`session_id`";
//    public static final String col_task_id = "`task_id`";
//    public static final String col_folder_name = "`folder_name`";
//    public static final String col_address = "`address`";
//    public static final String col_node = "`node`";
//    public static final String col_display_name = "`user_display_name`";
//    public static final String col_logo = "`logo`";
//    public static final String col_session_name = "`session_name`";
//
//    public static final String col_change_type = "`" + SdkNames.CHANGE_TYPE + "`";
//    public static final String col_change_source = "`" + SdkNames.CHANGE_SOURCE + "`";
//    public static final String col_change_target = "`" + SdkNames.CHANGE_TARGET + "`";
//    public static final String col_node_bytesize = "`" + SdkNames.CHANGE_NODE_BYTESIZE + "`";
//    public static final String col_change_md5 = "`" + SdkNames.CHANGE_NODE_MD5 + "`";
//    public static final String col_change_mtime = "`" + SdkNames.CHANGE_NODE_MTIME + "`";
//    public static final String col_change_node_path = "`" + SdkNames.CHANGE_NODE_PATH + "`";
//    public static final String col_change_location = "`location`";
//    public static final String col_change_node_id = "`" + SdkNames.CHANGE_NODE_ID + "`";
//    public static final String col_task_state = "`state`";
//    public static final String col_error_source = "`error_source`";
//    public static final String col_error = "`error`";


//    private final static String create_changes = "" +
//            "CREATE TABLE " + changes + "( " +
//            col_change_seq + " INTEGER, " +
//            col_change_node_id + " INTEGER, " +
//            col_change_type + " TEXT, " +
//            col_change_source + " TEXT, " +
//            col_change_target + " TEXT, " +
//            col_node_bytesize + " INTEGER," +
//            col_change_md5 + " TEXT, " +
//            col_change_mtime + " INTEGER, " +
//            col_change_node_path + "TEXT, " +
//            col_workspace + " TEXT NOT NULL, " +
//            col_change_location + " TEXT NOT NULL" +
//            ");",
//
//    create_properties =
//            "CREATE TABLE " + properties + "(" +
//                    col_name + " TEXT PRIMARY KEY NOT NULL," +
//                    col_value + " TEXT);",
//
//    create_certificate =
//            "CREATE TABLE " + certificates + "(" +
//                    col_alias + " TEXT PRIMARY KEY NOT NULL, " +
//                    col_certificate + " BLOB NOT NULL); ",
//
//    create_seq =
//            "CREATE TABLE " + seqs + "(" +
//                    col_workspace + " TEXT PRIMARY KEY NOT NULL, " +
//                    col_change_seq + " INTEGER" +
//                    ");";
//

//    private final static String deleteAccountSQL = String.format("delete from %s where %s=?;", sessions, col_sid);
//    private final static String getAccountSQL = String.format("select %s from %s where %s=?;", col_blob, sessions, col_sid);
//    private final static String saveAccountSQL = String.format("insert into %s values(?, ?);", sessions);
//
//    private final static String saveJWTSQL = String.format("insert into %s values (?, ?);", tokens);
//    private final static String delJWTSQL = String.format("delete from %s where %s=?;", tokens, col_sid);

    /* CERTIFICATES */

/*
    public X509Certificate getCertificate(String alias) {
        synchronized (lock) {
            try {
                try (Cursor cursor = MainDB.getHelper().getReadableDatabase().query(certificates, new String[]{col_certificate}, col_alias + "=?", new String[]{alias}, null, null, null)) {
                    if (!cursor.moveToFirst()) {
                        return null;
                    }
                    byte[] certificateBytes = cursor.getBlob(0);
                    ByteArrayInputStream bis = new ByteArrayInputStream(certificateBytes);
                    ObjectInput in = new ObjectInputStream(bis);
                    X509Certificate cert = (X509Certificate) in.readObject();
                    bis.close();
                    cursor.close();
                    return cert;
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    public static void saveCertificate(String alias, X509Certificate cert) {
        getHelper().addCertificate(alias, cert);
    }

    public void addCertificate(String alias, X509Certificate cert) {
        synchronized (lock) {
            try {
                SQLiteDatabase db = MainDB.getHelper().getWritableDatabase();
                String sql = "INSERT INTO " + certificates + " values(?, ?)";
                SQLiteStatement s = db.compileStatement(sql);
                s.bindString(1, alias);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutput out = new ObjectOutputStream(bos);
                out.writeObject(cert);
                byte[] data = bos.toByteArray();
                bos.close();
                s.bindBlob(2, data);
                s.execute();
            } catch (Exception ignored) {
            }
        }
    }

    public static List<X509Certificate> getTrustedCertificates() {
        synchronized (lock) {
            List<X509Certificate> certificateList = new ArrayList<>();
            try {
                try (Cursor cursor = MainDB.getHelper().getReadableDatabase().query(certificates, new String[]{col_certificate}, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        byte[] certificateBytes = cursor.getBlob(0);
                        ByteArrayInputStream bis = new ByteArrayInputStream(certificateBytes);
                        ObjectInput in = new ObjectInputStream(bis);
                        X509Certificate cert = (X509Certificate) in.readObject();
                        bis.close();
                        certificateList.add(cert);
                    }
                }
            } catch (Exception ignored) {
            }
            return certificateList;
        }
    }
*/


}
