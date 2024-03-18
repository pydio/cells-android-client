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
        }
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