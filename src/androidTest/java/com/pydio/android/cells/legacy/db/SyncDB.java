package com.pydio.android.cells.legacy.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.pydio.android.cells.legacy.db.model.WatchInfo;
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

//    public static List<WatchInfo> getActive() {
//        List<WatchInfo> list = new ArrayList<>();
//        SQLiteDatabase db = get().getReadableDatabase();
//        try (Cursor c = db.rawQuery("select session_id, workspace_label, add_time, last_sync_time, encoded, active from watched where active=1;", null)) {
//            while (c.moveToNext()) {
//                WatchInfo info = new WatchInfo();
//                info.setAccountID(c.getString(0));
//                info.setWorkspaceLabel(c.getString(1));
//                info.setAddTime(c.getLong(2));
//                info.setLastSyncTime(c.getLong(3));
//                String encoded = c.getString(4);
//
//                info.setNode(gson.fromJson(encoded, FileNode.class));
//                info.setActive(c.getInt(5) == 1);
//                list.add(info);
//            }
//        }
//        return list;
//    }
//
//    public static WatchInfo getWatchInfo(String accountID, String workspaceSlug, String path) {
//        SQLiteDatabase db = get().getReadableDatabase();
//        try (Cursor c = db.rawQuery("select session_id, workspace_label, add_time, last_sync_time, encoded, active from watched where session_id=? and workspace_slug=? and path=?;",
//                new String[]{accountID, workspaceSlug, path})) {
//            if (c.moveToNext()) {
//                WatchInfo info = new WatchInfo();
//                info.setAccountID(c.getString(0));
//                info.setWorkspaceLabel(c.getString(1));
//                info.setAddTime(c.getLong(2));
//                info.setLastSyncTime(c.getLong(3));
//                String encoded = c.getString(4);
//
//                info.setNode(gson.fromJson(encoded, FileNode.class));
//                info.setActive(c.getInt(5) == 1);
//                return info;
//            }
//            return null;
//        }
//    }

    // public static MessageListener<Status> messageListener;
//    public static CompleteListener completeListener;
//    public static FailureListener failureListener;
//    public static OfflineTaskStatusListener taskStatusListener;
//
//    public static final int watched = 1;
//    public static final int underWatched = 2;
//    public static final int notWatched = 3;
//
//    public static final String sessionID = "sync_session_id";
//    public static final String workspaceSlug = "sync_workspace_slug";
//    public static final String root = "sync_root_path";
//    public static final String error = "sync_error";
//    public static final String lastSyncTime = "sync_last_update";
//
//    public static final String autoSynchronizeHourRate = "offline_auto_sync_rate";
//    public static final String autoSynchronizeDayStart = "offline_auto_sync_day_start";

    // final public static OfflineWorker worker = new OfflineWorker();


    /*
    public static void scheduleNextAutoSync() {
        Context context = App.context();

        // todo: later find a way to load default value in a reliable way
        int rate = 12;
        String syncRateValue = App.getPreference(autoSynchronizeHourRate);
        if (syncRateValue != null && !syncRateValue.isEmpty()) {
            rate = Integer.parseInt(syncRateValue);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, rate);
        long triggerTime = calendar.getTimeInMillis();

        Intent intent = new Intent(context, FileSyncSignalReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private static void scheduleNextAutoSyncAt(long at) {
        Context context = App.context();

        Intent intent = new Intent(context, FileSyncSignalReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, at, pendingIntent);
        }
    }

    public static void synchronizeAllActive() {
        List<WatchInfo> activeTasks = getActive();
        for (WatchInfo info : activeTasks) {
            FileSyncTask task = new FileSyncTask(info);
            task.onFailure(failureListener);
            task.onEvent(messageListener);
            task.onComplete(completeListener);
            worker.push(task);
        }
    }

    public static void autoSyncAction() {
        List<WatchInfo> activeTasks = getActive();
        if (activeTasks.size() == 0) {
            return;
        }

        int startHour;

        String startHourValue = App.getPreference(autoSynchronizeDayStart);
        startHour = Integer.parseInt(startHourValue);

        Calendar currentDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        startDate.set(Calendar.HOUR, startHour);

        Calendar endDate = Calendar.getInstance();
        endDate.set(Calendar.HOUR, 24);

        if (currentDate.before(startDate) || currentDate.after(endDate)) {
            if (currentDate.before(startDate)) {
                scheduleNextAutoSyncAt(startDate.getTimeInMillis());
                return;
            }

            if (currentDate.after(endDate)) {
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                tomorrow.set(Calendar.HOUR_OF_DAY, startHour);
                scheduleNextAutoSyncAt(tomorrow.getTimeInMillis());
            }
        } else {
            for (WatchInfo info : activeTasks) {
                FileSyncTask task = new FileSyncTask(info);
                task.onFailure(failureListener);
                task.onEvent(messageListener);
                task.onComplete(completeListener);
                worker.push(task);
            }
            scheduleNextAutoSync();
        }
    }

    public static int getTaskStatus(WatchInfo info) {
        return worker.getStatus(info);
    }

    static void publishSyncEvent(Status status) {
        if (messageListener != null) {
            messageListener.onMessage(status);
        }
    }


    public static void addWatch(WatchInfo info, boolean autoSync) {
        int beforeAddCount = getActive().size();

        SQLiteDatabase db = get().getWritableDatabase();
        final String accountID = info.getAccountID();
        final FileNode node = info.getNode();
        final String workspaceSlug = node.getWorkspaceSlug();
        final String path = node.getPath();

        // Set main info
        String encoded = gson.toJson(node);
        if (getWatchState(accountID, workspaceSlug, path) == notWatched) {
            db.execSQL("insert into watched values (?, ?, ?, ?, ?, ?, ?, ?);",
                    new String[]{accountID, workspaceSlug, path, info.getWorkspaceLabel(), encoded, String.valueOf(info.getAddTime()), String.valueOf(info.getLastSyncTime()), "1"});
        }

        // Set stats
        SyncStats stats = info.getLastSyncStats();
        if (stats != null) {
            String encodedStats = gson.toJson(stats);
            if (hasStats(accountID, workspaceSlug, path)) {
                db.execSQL("insert into stats values (?, ?, ?, ?);",
                        new String[]{accountID, workspaceSlug, path, encodedStats});
            } else {
                db.execSQL("update stats set encoded_stats=? where session_id=? and workspace_slug=? and path=?;",
                        new String[]{encodedStats, accountID, workspaceSlug, path});

            }
        } else {
            db.execSQL("delete from stats  where session_id=? and workspace_slug=? and path=?;",
                    new String[]{accountID, workspaceSlug, path});
        }


        // Set errors
        SyncError error = info.getLastSyncErrorDetails();
        if (error != null) {
            String encodedError = gson.toJson(error);
            if (hasErrors(accountID, workspaceSlug, path)) {
                db.execSQL("insert into errors values (?, ?, ?, ?, ?, ?);",
                        new String[]{accountID, workspaceSlug, path, String.valueOf(error.getAction()), encodedError});
            } else {
                db.execSQL("update errors set encoded_stats=? where session_id=? and workspace_slug=? and path=?;",
                        new String[]{encodedError, accountID, workspaceSlug, path});

            }
        } else {
            db.execSQL("delete from errors where session_id=? and workspace_slug=? and path=?;",
                    new String[]{accountID, workspaceSlug, path});
        }


        if (autoSync) {
            // Add sync task in the main queue for this info
            FileSyncTask task = new FileSyncTask(info);
            task.onFailure(failureListener);
            task.onEvent(messageListener);
            task.onComplete(completeListener);
            worker.push(task);
        }

        // Schedule next sync if active list was empty before adding this info
        if (beforeAddCount == 0) {
            scheduleNextAutoSync();
        }
    }
*/




    /*

    public static SyncStats getStats(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getReadableDatabase();
        try (Cursor c = db.rawQuery("select encoded_stats from stats where session_id=? and workspace_slug=? and path=?;",
                new String[]{accountID, workspaceSlug, path})) {
            if (c.moveToNext()) {
                String encodedStats = c.getString(0);
                return gson.fromJson(encodedStats, SyncStats.class);
            }
        }
        return null;
    }

    public static void setStats(WatchInfo info, SyncStats stats) {
        SQLiteDatabase db = get().getWritableDatabase();
        final String accountID = info.getAccountID();
        final String workspaceSlug = info.getNode().getWorkspaceSlug();
        final String path = info.getNode().getPath();

        String encodedStats = gson.toJson(stats);
        if (hasStats(accountID, workspaceSlug, path)) {
            db.execSQL("update stats set encoded_stats=? where session_id=? and workspace_slug=? and path=?;",
                    new String[]{encodedStats, accountID, workspaceSlug, path});
        } else {
            db.execSQL("insert into stats values (?, ?, ?, ?);",
                    new String[]{accountID, workspaceSlug, path, encodedStats});
        }
    }

    public static void setStats(String accountID, String workspaceSlug, String path, SyncStats stats) {
        SQLiteDatabase db = get().getWritableDatabase();
        String encodedStats = gson.toJson(stats);
        if (hasStats(accountID, workspaceSlug, path)) {
            db.execSQL("update stats set encoded_stats=? where session_id=? and workspace_slug=? and path=?;",
                    new String[]{encodedStats, accountID, workspaceSlug, path});
        } else {
            db.execSQL("insert into stats values (?, ?, ?, ?);",
                    new String[]{accountID, workspaceSlug, path, encodedStats});
        }
    }

    public static void setError(WatchInfo info, SyncError details) {
        final String accountID = info.getAccountID();
        final String workspaceSlug = info.getNode().getWorkspaceSlug();
        final String path = info.getNode().getPath();

        SQLiteDatabase db = get().getWritableDatabase();
        String encodedError = gson.toJson(details);
        if (hasErrors(accountID, workspaceSlug, path)) {
            db.execSQL("update errors set encoded_error=? where session_id=? and workspace_slug=? and path=?;",
                    new String[]{encodedError, accountID, workspaceSlug, path});
        } else {
            db.execSQL("insert into errors values (?, ?, ?, ?, ?);",
                    new String[]{accountID, workspaceSlug, path, String.valueOf(details.action), encodedError});
        }
    }

    public static void setError(String accountID, String workspaceSlug, String path, SyncError details) {
        SQLiteDatabase db = get().getWritableDatabase();
        String encodedError = gson.toJson(details);
        if (hasErrors(accountID, workspaceSlug, path)) {
            db.execSQL("update errors set encoded_error=? where session_id=? and workspace_slug=? and path=?;",
                    new String[]{encodedError, accountID, workspaceSlug, path});
        } else {
            db.execSQL("insert into errors values (?, ?, ?, ?, ?);",
                    new String[]{accountID, workspaceSlug, path, String.valueOf(details.action), encodedError});
        }
    }

    public static SyncError getErrorDetails(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select encoded_error from errors where session_id=? and workspace_slug=? and path=?;",
                new String[]{accountID, workspaceSlug, path})) {
            if (cursor.moveToNext()) {
                String encoded = cursor.getString(0);
                return gson.fromJson(encoded, SyncError.class);
            }
        }
        return null;
    }

    public static int getWatchState(String accountID, String workspaceSlug, String path) {
        try (Cursor c = get().getReadableDatabase().rawQuery("select path from watched where session_id=? and workspace_slug=? order by path;",
                new String[]{accountID, workspaceSlug})) {
            while (c.moveToNext()) {
                String item = c.getString(0);
                if (item.equals(path)) {
                    return watched;
                }

                if (path.startsWith(item + "/")) {
                    return underWatched;
                }
            }
            return notWatched;
        }
    }

    public static boolean hasStats(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select * from stats where session_id=? and workspace_slug=? and path=?;",
                new String[]{accountID, workspaceSlug, path})) {
            return cursor.getCount() > 0;
        }
    }

    public static boolean hasErrors(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select * from errors where session_id=? and workspace_slug=? and path=?;",
                new String[]{accountID, workspaceSlug, path})) {
            return cursor.getCount() > 0;
        }
    }

    public static void deleteWatch(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("delete from watched where session_id=? and workspace_slug=? and path=?;", new String[]{accountID, workspaceSlug, path});
        db.execSQL("delete from stats where session_id=? and workspace_slug=? and path=?;", new String[]{accountID, workspaceSlug, path});
        db.execSQL("delete from errors where session_id=? and workspace_slug=? and path=?;", new String[]{accountID, workspaceSlug, path});
    }

    public static void activateForSession(String accountID) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("update watched set active=1 where session_id=? and active=0;", new String[]{accountID});
    }

    public static void deActivateForSession(String accountID) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("update watched set active=0 where session_id=? and active=1;", new String[]{accountID});
    }

    public static boolean isActive(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getReadableDatabase();
        try (Cursor cursor = db.rawQuery("select active from watched where session_id=? and workspace_slug=? and path=?;", new String[]{accountID, workspaceSlug, path})) {
            if (!cursor.moveToFirst()) {
                return false;
            }
            return cursor.getInt(0) == 1;
        }
    }

    public static void setLastSyncTime(String accountID, String workspaceSlug, String path, long time) {
        get().getWritableDatabase().execSQL("update watched set last_sync_time=? where session_id=? and workspace_slug=? and path=?;",
                new String[]{"" + time, accountID, workspaceSlug, path});
    }

    public static void deleteError(String accountID, String workspaceSlug, String path) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("delete from errors where session_id=? and workspace_slug=? and path=?;",
                new String[]{accountID, workspaceSlug, path});
        db.execSQL("delete from errors where session_id=? and workspace_slug=? and path like ?;",
                new String[]{accountID, workspaceSlug, path + "/%"});
    }


    public static void deleteForSession(String accountID) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("delete from watched where session_id=?;",
                new String[]{accountID});

        db.execSQL("delete from errors where session_id=?;",
                new String[]{accountID});

        db.execSQL("delete from stats where session_id=?;",
                new String[]{accountID});
    }

    public static void updateSessionID(String oldValue, String newValue) {
        SQLiteDatabase db = get().getWritableDatabase();
        db.execSQL("update watched set session_id=? where session_id=?;",
                new String[]{newValue, oldValue});

        db.execSQL("update errors set session_id=? where session_id=?;",
                new String[]{newValue, oldValue});

        db.execSQL("update stats set session_id=? where session_id=?;",
                new String[]{newValue, oldValue});
    }

     */
}