package com.pydio.android.cells.legacy.db.model;

import com.pydio.cells.api.ui.FileNode;

public class WatchInfo {

    private String accountID;
    private String workspaceLabel = "";
    private FileNode node;
    private long addTime;
    private long lastSyncTime;
    private SyncStats lastSyncStats;
    private SyncError lastSyncErrorDetails;

    private boolean active = true;

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getWorkspaceLabel() {
        return workspaceLabel;
    }

    public void setWorkspaceLabel(String workspaceLabel) {
        this.workspaceLabel = workspaceLabel;
    }

    public FileNode getNode() {
        return node;
    }

    public void setNode(FileNode node) {
        this.node = node;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WatchInfo)) {
            return false;
        }

        return accountID.equals(((WatchInfo) object).accountID) &&
                node.getWorkspace().equals(((WatchInfo) object).node.getWorkspace()) &&
                node.getPath().equals(((WatchInfo) object).node.getPath());
    }

    public SyncStats getLastSyncStats() {
        return lastSyncStats;
    }

    public void setLastSyncStats(SyncStats lastSyncStats) {
        this.lastSyncStats = lastSyncStats;
    }

    public SyncError getLastSyncErrorDetails() {
        return lastSyncErrorDetails;
    }

    public void setLastSyncErrorDetails(SyncError lastSyncErrorDetails) {
        this.lastSyncErrorDetails = lastSyncErrorDetails;
    }

}
