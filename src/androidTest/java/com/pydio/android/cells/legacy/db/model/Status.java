package com.pydio.android.cells.legacy.db.model;

import androidx.annotation.NonNull;

public class Status {

    protected String accountID;
    protected String workspaceSlug;
    protected String rootPath;
//    protected Event event;

    public Status() {
    }

    protected Status(String rootPath, String accountID, String slug) {
        this.rootPath = rootPath;
        this.accountID = accountID;
        this.workspaceSlug = slug;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getWorkspaceSlug() {
        return workspaceSlug;
    }

    public String getRootPath() {
        return rootPath;
    }

//    public Event getEvent() {
//        return event;
//    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public void setWorkspaceSlug(String workspaceSlug) {
        this.workspaceSlug = workspaceSlug;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

//    public void setEvent(Event event) {
//        this.event = event;
//    }

    @NonNull
    @Override
    public String toString() {
//        if (event != null) {
//            return event.toString();
//        }
        return "MESSAGE: From root -> (" + accountID + ", " + workspaceSlug + ", " + rootPath + ")";
    }
}
