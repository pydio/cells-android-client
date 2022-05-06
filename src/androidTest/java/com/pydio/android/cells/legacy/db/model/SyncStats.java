package com.pydio.android.cells.legacy.db.model;

public class SyncStats {
    int deleted;
    int created;
    int updated;
    int failures;
    int total;

    long deletedSize;
    long downloadedSize;

    long totalSize;

    long time;

    public int getDeleted() {
        return deleted;
    }

    public int getCreated() {
        return created;
    }

    public int getFailures() {
        return failures;
    }

    public int getUpdated() {
        return updated;
    }

    public int getTotal() {
        return total;
    }

    public long getDeletedSize() {
        return deletedSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getTime() {
        return time;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setDeletedSize(long deletedSize) {
        this.deletedSize = deletedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void setTime(long time) {
        this.time = time;
    }
}


