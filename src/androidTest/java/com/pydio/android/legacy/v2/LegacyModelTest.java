package com.pydio.android.legacy.v2;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.pydio.cells.transport.auth.Token;
import com.pydio.cells.utils.Log;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LegacyModelTest {

    private final String logTag = LegacyModelTest.class.getSimpleName();

    @Test
    public void checkModel() {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Main DB
        String mainDbPath = context.getDataDir().getAbsolutePath() + V2MainDB.DB_FILE_PATH;
        File mainDbFile = new File(mainDbPath);
        if (mainDbFile.exists()) {
            Log.i(logTag, "... Found a legacy main DB");
            V2MainDB.init(context, mainDbPath);
            V2MainDB accDB = V2MainDB.getHelper();
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
        String syncDbPath = context.getDataDir().getAbsolutePath() + V2SyncDB.DB_FILE_PATH;
        File syncDbFile = new File(syncDbPath);
        if (syncDbFile.exists()) {
            Log.i(logTag, "... Found a legacy sync DB");
            V2SyncDB.init(context, syncDbPath);
            V2SyncDB v2SyncDB = V2SyncDB.getHelper();
            List<WatchInfo> infos = v2SyncDB.getAll();
            Log.w(logTag, "... Found " + infos.size() + " offline roots. ");
            for (WatchInfo offlineRoot : infos) {
                Log.w(logTag, "- " + offlineRoot.getAccountID() + " @ " + offlineRoot.getWorkspaceLabel() + ": " + offlineRoot.getNode().getPath());
            }
        }

    }
}
