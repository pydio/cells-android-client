package com.pydio.android.cells.integration.app;

import com.pydio.android.cells.test.DebugNodeHandler;
import com.pydio.android.cells.test.LocalTestUtils;
import com.pydio.android.cells.test.TestClientFactory;
import com.pydio.cells.api.Client;
import com.pydio.cells.api.SdkNames;
import com.pydio.cells.api.Transport;
import com.pydio.cells.api.ui.FileNode;
import com.pydio.cells.utils.Log;
import com.pydio.cells.utils.tests.RemoteServerConfig;
import com.pydio.cells.utils.tests.TestConfiguration;
import com.pydio.cells.utils.tests.TestUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class CellsClientTest {

    private final String logTag = CellsClientTest.class.getSimpleName();

    private TestClientFactory factory;
    private TestConfiguration config;
    private String testRunID;
    private RemoteServerConfig cellsConf;

    @Before
    public void setup() {
        testRunID = LocalTestUtils.getRunID();
        factory = new TestClientFactory();
        config = TestConfiguration.getDefault();
        cellsConf = config.getServer("cells-https");
    }

    @Test
    public void testBookmark() throws Exception {

        if (cellsConf == null) {
            Log.w(logTag, "No Pydio Cells configuration found, skipping");
            return;
        }

        Transport cellsTransport = TestUtils.getTransport(factory, cellsConf);
        Client client = factory.getClient(cellsTransport);

        // Upload
        String baseDir = "/";
        String fileName = "hello-" + testRunID + ".txt";
        String file = baseDir + fileName;

        String message = "Hello Pydio! - this is a message from test run #" + testRunID;
        byte[] content = message.getBytes();
        ByteArrayInputStream source = new ByteArrayInputStream(content);

//        Message msg =
        client.upload(source, content.length, "text/plain", cellsConf.defaultWS, baseDir, fileName, true, (progress) -> {
            System.out.printf("\r... %d bytes written\n", progress);
            return "";
        });
//        Assert.assertNotNull(msg);
//        Assert.assertEquals("SUCCESS", msg.type());

        // Post with a stat
        FileNode node = client.nodeInfo(cellsConf.defaultWS, file);
        String uuid = node.getProperty(SdkNames.NODE_PROPERTY_UID);
        String nodePath = node.getProperty(SdkNames.NODE_PROPERTY_PATH);
        Log.w(logTag, "... Stats request succeeded");
        Log.w(logTag, "UUID: " + uuid);
        Log.w(logTag, "Path: " + nodePath);

        // Bookmark file
        client.bookmark(cellsConf.defaultWS, file, true);
        client.getBookmarks(new DebugNodeHandler(logTag));
    }

}
