package com.pydio.android.cells.legacy.db.model;

public class SyncError {
    final int action;
    final ErrorInfo info;

    public SyncError(int action, ErrorInfo info) {
        this.action = action;
        this.info = info;
    }

    public int getAction() {
        return action;
    }

    public ErrorInfo getInfo() {
        return info;
    }
}
