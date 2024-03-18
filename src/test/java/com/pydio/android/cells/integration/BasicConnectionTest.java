package com.pydio.android.cells.integration;

import com.pydio.android.cells.test.DebugNodeHandler;
import com.pydio.android.cells.test.TestClientFactory;
import com.pydio.cells.api.Client;
import com.pydio.cells.api.SDKException;
import com.pydio.cells.api.ServerURL;
import com.pydio.cells.api.Transport;
import com.pydio.cells.transport.ServerURLImpl;
import com.pydio.cells.transport.StateID;
import com.pydio.cells.utils.Log;
import com.pydio.cells.utils.tests.RemoteServerConfig;
import com.pydio.cells.utils.tests.TestConfiguration;
import com.pydio.cells.utils.tests.TestUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

/**
 * Performs basic tests against a running Pydio Server instance (P8 or Cells).
 * You must first adapt the "src/test/resources/accounts/*.properties" files
 * (at least one of them) to match your setup.
 * <p>
 * You can then launch the test with:
 * <p>
 * {@code ./gradlew build -Dtest.profile=integration}
 */
public class BasicConnectionTest {

    private final static String logTag = BasicConnectionTest.class.getSimpleName();

    private TestClientFactory factory;
    private TestConfiguration config;
    private String testRunID;

    @Before
    public void setup() {
        testRunID = TestUtils.randomString(4);
        config = TestConfiguration.getDefault();
        factory = new TestClientFactory();
    }

    @After
    public void teardown() {
        // do nothing
    }

    @Test
    public void testSimpleCRUD() throws SDKException, IOException {
        Map<String, RemoteServerConfig> servers = config.getDefinedServers();

        if (servers.isEmpty()) {
            Log.w(logTag, "No configuration found, skipping");
            return;
        }

        for (RemoteServerConfig conf : servers.values()) {
            Log.i(logTag, "... Launching Simple Crud on " + conf.serverURL);
            testWorkspaces(conf);
            basicCRUD(conf);
        }
    }

    private void testWorkspaces(RemoteServerConfig conf) throws SDKException {

        Transport transport = TestUtils.getTransport(factory, conf);

        System.out.println("... Listing workspaces for " + printableId(transport.getId()));
        factory.getClient(transport).workspaceList(new DebugNodeHandler(logTag));

        System.out.println("... Listing object for workspace " + conf.defaultWS);
        factory.getClient(transport).ls(conf.defaultWS, "/", null, (node) -> System.out.println(node.getName()));
    }

    public void basicCRUD(RemoteServerConfig conf) throws SDKException, IOException {
        // System.out.println("... Testing CRUD for " + printableId(transport.getId()));

        Transport transport = TestUtils.getTransport(factory, conf);
        Client client = factory.getClient(transport);

        System.out.println("... Testing CRUD for " + printableId(transport.getId()));

        String baseDir = "/";
        final String name = "hello-" + testRunID + ".txt";
        String message = "Hello Pydio! - this is a message from test run #" + testRunID;

        // Upload
        byte[] content = message.getBytes();
        ByteArrayInputStream source = new ByteArrayInputStream(content);
//        Message msg =
        client.upload(source, content.length, "text/plain",
                conf.defaultWS, baseDir, name, true, (progress) -> {
                    System.out.printf("\r%d bytes written\n", progress);
                    return "";
                });
//        Assert.assertNotNull(msg);
//        Assert.assertEquals("SUCCESS", msg.type());

        // Read
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            client.download(conf.defaultWS, baseDir + name, out, null);
            out.flush();
            String retrievedMsg = out.toString(StandardCharsets.UTF_8);
            System.out.println("Retrieved: " + retrievedMsg);
            Assert.assertEquals(message, retrievedMsg);
        }

        // Update fails in P8
        if (!transport.getServer().isLegacy()) {
            message += " -- Add with some additional content";
            content = message.getBytes();
            source = new ByteArrayInputStream(content);
//            msg =
            client.upload(source, content.length, "text/plain",
                    conf.defaultWS, baseDir, name, true, (progress) -> {
                        System.out.printf("\r%d bytes written\n", progress);
                        return "";
                    });
//            Assert.assertNotNull(msg);
//            Assert.assertEquals("SUCCESS", msg.type());

            // Read updated
            String retrievedMsg = "";
            for (int i = 0; i < 10; i++) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    client.download(conf.defaultWS, baseDir + name, out, null);
                    out.flush();
                    retrievedMsg = out.toString(StandardCharsets.UTF_8);
                    System.out.println("Retrieved: " + retrievedMsg);

                    if (message.equals(retrievedMsg)) {
                        break;
                    }
                    Thread.sleep(2000);
                    System.out.println("Wait 2s before retry...");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Assert.assertEquals(message, retrievedMsg);
        }

        client.delete(conf.defaultWS, new String[]{"/" + name});

        boolean deleted = false;
        for (int i = 0; i < 10; i++) {
            try {

                // Check if uploaded files is still there
                final List<String> founds = new ArrayList<>();
                client.ls(conf.defaultWS, baseDir, null, (node) -> {
                    if (name.equals(node.getName()))
                        founds.add(name);
                });
                if (founds.size() == 0) {
                    deleted = true;
                    break;
                }
                Thread.sleep(2000);
                System.out.println("Wait 2s before retry...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Assert.assertTrue(deleted);

        // TODO finish implementing the CRUD and corresponding checks.
    }

    @Test
    public void testSkipVerify() throws Exception {

        Map<String, RemoteServerConfig> servers = config.getDefinedServers();
        for (String key : servers.keySet()) {

            RemoteServerConfig currConf = servers.get(key);
            if (!currConf.skipVerify) {
                continue;
            }

            // Test Self signed URL
            ServerURL currURL;
            try {
                currURL = ServerURLImpl.fromAddress(currConf.serverURL);
                // => Self-signed: ping fails
                currURL.ping();
            } catch (Exception e) {
                Assert.assertTrue(e instanceof SSLHandshakeException);
                try {
                    currURL = ServerURLImpl.fromAddress(currConf.serverURL, true);
                    currURL.ping();
                } catch (Exception e2) {
                    Assert.assertNull(e);
                }
            }
        }
    }

    private String printableId(String techId) {
        return StateID.fromId(techId).toString();
    }

}