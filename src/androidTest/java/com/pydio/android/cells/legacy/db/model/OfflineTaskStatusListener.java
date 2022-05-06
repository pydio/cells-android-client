package com.pydio.android.cells.legacy.db.model;

public interface OfflineTaskStatusListener {
    void onTaskStatusUpdated(WatchInfo info, int status);
}
