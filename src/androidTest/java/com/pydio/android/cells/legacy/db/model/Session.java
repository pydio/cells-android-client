package com.pydio.android.cells.legacy.db.model;

import android.os.Environment;

import com.pydio.cells.api.SdkNames;
import com.pydio.cells.api.Server;
import com.pydio.cells.api.ServerURL;
import com.pydio.cells.api.Transport;
import com.pydio.cells.api.ui.FileNode;
import com.pydio.cells.api.ui.Node;
import com.pydio.cells.api.ui.WorkspaceNode;

import java.io.File;

public class Session {

    // TODO rather use an ENUM
    public final static String STATUS_IDLE = "idle";
    public final static String STATUS_LOADING = "loading";
    public final static String STATUS_ONLINE = "online";
    public final static String STATUS_ERROR = "error";

    private String status = "idle"; // "idle", "loading", "online", "error"

    private final AccountRecord persistedAccount;

    private ServerURL serverURL;
    private Server server;

    private Transport transport;

    //private SessionFactory factory;

    public Session(AccountRecord account) {
        this.persistedAccount = account;
    }

    public Session(AccountRecord account, Transport transport) {
        // this.factory = factory;
        this.persistedAccount = account;
        this.transport = transport;
        status = STATUS_ONLINE;
    }

    public String id() {
        return persistedAccount.id();
    }

    public String getStatus() {
        return status;
    }

    public AccountRecord getAccount() {
        return persistedAccount;
    }

    public String getUser() {
        return persistedAccount.getUsername();
    }

    public Server getServer() {
        return server;
    }

    public boolean isLegacy() {
        return persistedAccount.isLegacy();
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOnline(ServerURL serverURL, Server server, Transport transport) {
        this.serverURL = serverURL;
        this.server = server;
        this.transport = transport;
        this.status = STATUS_ONLINE;
    }

    public boolean isOnline() {
        return STATUS_ONLINE.equals(status);
    }

    public Transport getTransport() {
        return transport;
    }

    @Deprecated
    public String workspaceSlug(Node node) {
        // TODO move somewhere else
        if (node.getType() == Node.TYPE_WORKSPACE) {
            return node.getId();
        } else {
            String slug = node.getProperty(SdkNames.NODE_PROPERTY_WORKSPACE_SLUG);
            if (slug == null || "".equals(slug)) {
                throw new RuntimeException("Retrieving a workspace by UUID is not yet implemented");
                // WorkspaceNode ws = sessionNode.server().findWorkspaceById(node.getProperty(SdkNames.NODE_PROPERTY_WORKSPACE_UUID));
                //if (ws != null) {
                //    slug = ws.getSlug();
                //}
            }
            return slug;
        }
    }

    @Deprecated
//    public void downloadURL(Node node, StringCompletion completion) {
//        // TODO move somewhere else
//        Background.go(() -> {
//            String url = null;
//            Error error = null;
//            try {
//                url = getClient().downloadPath(workspaceSlug(node), node.getPath());
//            } catch (SDKException e) {
//                error = Error.fromException(e);
//            }
//            completion.onComplete(url, error);
//        });
//    }

    public WorkspaceNode resolveNodeWorkspace(FileNode node) {
        String slug = node.getProperty(SdkNames.NODE_PROPERTY_WORKSPACE_SLUG);
        if (slug != null && !"".equals(slug)) {
            return persistedAccount.getCachedWorkspace(slug);
        }
        return null;
    }

//    public static String externalBaseFolderPath(String sessionID) {
//        return Application.externalDir(null) + File.separator + sessionID;
//    }

    public static String publicDownloadPath(String label) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        String extension = "";
        String baseName = label;
        int i = label.lastIndexOf('.');
        if (i > 0) {
            extension = "." + label.substring(i + 1);
            baseName = label.substring(0, i);
        }

        i = 0;
        File file = new File(downloadDir, label);
        while (file.exists()) {
            i++;
            file = new File(downloadDir, baseName + "-" + i + extension);
        }
        return file.getPath();
    }

}
