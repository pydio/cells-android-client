package com.pydio.android.cells.legacy.db;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.pydio.android.cells.legacy.db.model.AccountRecord;
import com.pydio.android.cells.legacy.db.model.WatchInfo;
import com.pydio.cells.transport.auth.Token;
import com.pydio.cells.utils.Log;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LegacyModelTest {

    private final String logTag = LegacyModelTest.class.getSimpleName();

    @Test
    public void checkModel() throws Exception {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Main DB
        String mainDbPath = context.getDataDir().getAbsolutePath() + MainDB.DB_FILE_PATH;
        File mainDbFile = new File(mainDbPath);
        if (mainDbFile.exists()) {
            Log.i(logTag, "... Found a legacy main DB");
            MainDB.init(context, mainDbPath);
            MainDB accDB = MainDB.getHelper();
            List<AccountRecord> recs = accDB.listAccountRecords();
            Log.w(logTag, "... Found " + recs.size() + " accounts. ");
            for (AccountRecord rec : recs) {
                Log.w(logTag, "- " + rec.getUsername() + "@" + rec.url());
            }

            Map<String, Token> tokens = accDB.listAllTokens();
            Log.w(logTag, "... Found " + tokens.size() + " tokens. ");
            for (String key : tokens.keySet()) {
                Log.w(logTag, "- " + key + ": " + tokens.get(key).subject);
            }

            Map<String, String> pwds = accDB.listAllLegacyPasswords();
            Log.w(logTag, "... Found " + pwds.size() + " passwords. ");
            for (String key : pwds.keySet()) {
                Log.w(logTag, "- " + key + ": " + pwds.get(key));
            }
        }
        // Offline
        String syncDbPath = context.getDataDir().getAbsolutePath() + SyncDB.DB_FILE_PATH;
        File syncDbFile = new File(syncDbPath);
        if (syncDbFile.exists()) {
            Log.i(logTag, "... Found a legacy sync DB");
            SyncDB.init(context, syncDbPath);
            SyncDB syncDB = SyncDB.getHelper();
            List<WatchInfo> infos = syncDB.getAll();
            Log.w(logTag, "... Found " + infos.size() + " offline roots. ");
            for (WatchInfo offlineRoot : infos) {
                Log.w(logTag, "- " + offlineRoot.getAccountID() + " @ " + offlineRoot.getWorkspaceLabel() + ": " + offlineRoot.getNode().getPath());
            }
        }

    }
}
