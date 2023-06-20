package com.pydio.android.legacy.v2;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.pydio.cells.transport.auth.Token;
import com.pydio.cells.utils.Log;
import com.pydio.cells.utils.tests.RemoteServerConfig;
import com.pydio.cells.utils.tests.TestConfiguration;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class LegacyDBTest {

    private final String logTag = "LegacyDBTest";

    //     private TestClientFactory factory;
    private TestConfiguration config;
    private String testRunID;
    private RemoteServerConfig cellsConf;

    @Before
    public void setup() {
//        testRunID = LocalTestUtils.getRunID();
//        factory = new TestClientFactory();
//        config = TestConfiguration.getDefault();
//        cellsConf = config.getServer("cells-https");
    }

    @Test
    public void testSimpleList() throws Exception {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Main DB
        String mainDbPath = context.getDataDir().getAbsolutePath() + V2MainDB.DB_FILE_PATH;
        File mainDbFile = new File(mainDbPath);
        if (mainDbFile.exists()) {
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
            V2SyncDB.init(context, syncDbPath);
            V2SyncDB v2SyncDB = V2SyncDB.getHelper();
            List<WatchInfo> infos = v2SyncDB.getAll();
            Log.w(logTag, "... Found " + infos.size() + " offline roots. ");
            for (WatchInfo offlineRoot : infos) {
                Log.w(logTag, "- " + offlineRoot.getAccountID() + " @ " + offlineRoot.getWorkspaceLabel() + ": " + offlineRoot.getNode().getPath());
            }
        }

    }

//    public void doSomething() throws Exception {
//
//            Transport cellsTransport = TestUtils.getTransport(factory, cellsConf);
//        Client client = factory.getClient(cellsTransport);
//
//        // Upload
//        String baseDir = "/";
//        String fileName = "hello-" + testRunID + ".txt";
//        String file = baseDir + fileName;
//
//        String message = "Hello Pydio! - this is a message from test run #" + testRunID;
//        byte[] content = message.getBytes();
//        ByteArrayInputStream source = new ByteArrayInputStream(content);
//
//        Message msg = client.upload(source, content.length, "text/plain", cellsConf.defaultWS, baseDir, fileName, true, (progress) -> {
//            System.out.printf("\r... %d bytes written\n", progress);
//            return false;
//        });
//        Assert.assertNotNull(msg);
//        Assert.assertEquals("SUCCESS", msg.type());
//
//        // Post with a stat
//        FileNode node = client.nodeInfo(cellsConf.defaultWS, file);
//        String uuid = node.getProperty(SdkNames.NODE_PROPERTY_UID);
//        String nodePath = node.getProperty(SdkNames.NODE_PROPERTY_PATH);
//        System.out.println("... Stats request succeeded");
//        System.out.println("UUID: " + uuid);
//        System.out.println("Path: " + nodePath);
//
//        // Bookmark file
//        Message bookmarkMessage = client.bookmark(cellsConf.defaultWS, file, true);
//
//        client.getBookmarks(new DebugNodeHandler(logTag));
//    }

}
