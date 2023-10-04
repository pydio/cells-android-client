package com.pydio.android.legacy.v2;

import com.pydio.cells.api.ui.WorkspaceNode;

import java.util.Map;

/**
 * Additional object that defines what we store in the V2 MainDB
 * and can be easily serialized back and forth using gson.
 */
public class AccountRecord {

    String accountID;
    String username;
    String serverUrl;
    // TODO also accepted certificates here.
    boolean skipVerify;
    boolean legacy;

    // Also stores UI messages
    String serverLabel;
    String welcomeMessage;

    Map<String, WorkspaceNode> cachedWorkspaces;

    public String id() {
        return accountID;
    }

    public String getUsername() {
        return username;
    }

    public String url() {
        return serverUrl;
    }

    public boolean skipVerify() {
        return skipVerify;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setWorkspaces(Map<String, WorkspaceNode> cachedWorkspaces) {
        this.cachedWorkspaces = cachedWorkspaces;
    }

//    public static AccountRecord fromServer(String username, Server server) {
//        AccountRecord record = new AccountRecord();
//        record.username = username;
//        record.serverUrl = server.url();
//        // Not very elegant. Improve.
//        StateID state = new StateID(username, server.url());
//        record.accountID = state.getId();
//        record.skipVerify = server.getServerURL().skipVerify();
//        record.legacy = server.isLegacy();
//        record.serverLabel = server.getLabel();
//        record.welcomeMessage = server.getWelcomeMessage();
//        return record;
//    }
}
