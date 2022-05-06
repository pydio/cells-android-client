package com.pydio.android.cells.legacy.db.model;

import java.io.Serializable;
import java.util.Properties;

public class LegacyAccountRecord implements Serializable {

    public String ID;
    public Server server;
    public String user;

    public LegacyAccountRecord() {
    }

    public static class Server {
        public String host;
        public String scheme;
        public int port;
        public String path;
        public String version;
        public String versionName;
        public String iconURL;
        public String welcomeMessage;
        public String label;
        public String url;
        public boolean sslUnverified;
        public boolean legacy;
        public Properties properties;
    }
}
