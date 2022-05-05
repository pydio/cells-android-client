package com.pydio.android.cells.integration;

import com.pydio.cells.api.Server;
import com.pydio.cells.client.CellsClient;
import com.pydio.cells.utils.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility class to centralise setup of a Cells client for testing purposes.
 */
public class TestClient {

    private final static String logTag = TestClient.class.getSimpleName();

    private Server server;
    private CellsClient cellsClient;

    private Path workingDirPath;

    private String serverURL, login, pwd, workspace, skipVerify;

    public void setup() {

        URL url = TestClient.class.getResource("/accounts/default.properties");
        workingDirPath = Paths.get(url.getPath()).getParent();

        Properties p = new Properties();
        try (InputStream is = TestClient.class.getResourceAsStream("/accounts/default.properties")) {
            p.load(new InputStreamReader(is));
            serverURL = p.getProperty("serverURL");
            login = p.getProperty("login");
            pwd = p.getProperty("pwd");
            workspace = p.getProperty("defaultWorkspace");
            skipVerify = p.getProperty("skipVerify", "false");
        } catch (IOException e) {
            Log.e(logTag, "could not retrieve configuration file, cause: " + e.getMessage());
            return;
        }

//        node = new ServerNode();
//        Error error = node.resolve(serverURL);
//        if (error != null) {
//            String msg = "Could not resolve server URL, cause: " + error;
//            throw new RuntimeException(msg);
//        }
//
//        // We rather retrieve an Android Specific client
//        cellsClient = new CellsClient(node);
//        ClientFactory.setCellsFactory(new AndroidClientFactory());
//
//        Client c  = ClientFactory.get(node);
//        if (c instanceof CellsClient){
//            cellsClient = (CellsClient) ClientFactory.get(node);
//        } else {
//            throw new RuntimeException("Unimplemented test when the client is not an instance of CellsClient");
//        }
//
//        cellsClient.setCredentials(new LegacyPasswordCredentials(login, pwd));
//        cellsClient.setSkipOAuthFlag(true);
//
//        cellsFs = new CellsFs("test", cellsClient, workspace, stateManager);

    }

    public CellsClient getCellsClient() {
        return cellsClient;
    }

    public String getDefaultWorkspace() {
        return workspace;
    }

    public Path getWorkingDir() {
        return workingDirPath;
    }

}