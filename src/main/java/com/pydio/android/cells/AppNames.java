package com.pydio.android.cells;

public interface AppNames {

    // OAuth Code Flow when remote is Cells
    String QUERY_KEY_CODE = "code";
    String QUERY_KEY_STATE = "state";

    // For OAuth callback
    // String CELLS_ROOT_ENCODED_STATE = "cells%3A%2F%2Froot";

    // Network management
    String NETWORK_TYPE_UNMETERED = "Unmetered";
    String NETWORK_TYPE_METERED = "Metered";
    String NETWORK_TYPE_ROAMING = "Roaming";
    String NETWORK_TYPE_UNAVAILABLE = "Unavailable";
    String NETWORK_TYPE_UNKNOWN = "Unknown";
    String NETWORK_TYPE_CONSTRAINT_NONE = "None";
    String NETWORK_TYPE_CONSTRAINT_UNMETERED = "Unmetered";
    String NETWORK_TYPE_CONSTRAINT_NOT_ROAMING = "Not Roaming";

    /* SHARED PREFERENCE WELL KNOWN VALUES */

    // LAYOUT AND SORTS
    // Default sort orders
    String DEFAULT_SORT_ENCODED = "sort_name||ASC";
    String TRANSFER_DEFAULT_ENCODED_ORDER = "transfer_id||DESC";
    String JOB_DEFAULT_ENCODED_ORDER = "job_id||DESC";

    // OFFLINE SYNC
    String SYNC_FREQ_QUARTER = "quarter";
    String SYNC_FREQ_HOUR = "hour";
    String SYNC_FREQ_DAY = "day";
    String SYNC_FREQ_WEEK = "week";

    /* Generic actions */
    // Rather make this generic
    String KEY_PREFIX = "com.pydio.android.cells";
    String KEY_PREFIX_ = KEY_PREFIX + ".";

    String ACTION_MORE = KEY_PREFIX_ + "more";
    String ACTION_OPEN = KEY_PREFIX_ + "open";
    String ACTION_CANCEL = KEY_PREFIX_ + "cancel";
    String ACTION_DONE = KEY_PREFIX_ + "done";
    String ACTION_RESTART = KEY_PREFIX_ + "restart";
    String ACTION_CHOOSE_TARGET = KEY_PREFIX_ + "choosetarget";
    String ACTION_COPY = KEY_PREFIX_ + "copy";
    String ACTION_MOVE = KEY_PREFIX_ + "move";
    String ACTION_UPLOAD = KEY_PREFIX_ + "upload";
    String ACTION_DOWNLOAD = KEY_PREFIX_ + "download";
    String ACTION_CREATE_FOLDER = KEY_PREFIX_ + "createfolder";

    String ACTION_OPEN_PARENT_IN_WORKSPACES = KEY_PREFIX_ + "openparentinworkspaces";
    String ACTION_DELETE_RECORD = KEY_PREFIX_ + "deleterecord";

    String ACTION_LOGIN = "login";
    String ACTION_LOGOUT = "logout";
    String ACTION_FORGET = "forget";

    /* Flags */
    int FLAG_BOOKMARK = 1;
    int FLAG_SHARE = 2;
    int FLAG_OFFLINE = 4;
    int FLAG_HAS_THUMB = 8;
    int FLAG_PRE_VIEWABLE = 16;

    /* Modification status */
    String LOCAL_MODIF_DELETE = "deleting";
    String LOCAL_MODIF_RENAME = "renaming";
    String LOCAL_MODIF_MOVE = "moving";
    String LOCAL_MODIF_RESTORE = "restore";

    /* Async jobs */
    // Known owners
    String JOB_OWNER_USER = "User";
    String JOB_OWNER_WORKER = "Worker";
    String JOB_OWNER_SYSTEM = "Android System";
    // Known Templates
    String JOB_TEMPLATE_FULL_RESYNC = "full-sync";
    String JOB_TEMPLATE_RESYNC = "sync-%s";
    String JOB_TEMPLATE_MIGRATION_V2 = "migration-v2";
    String JOB_TEMPLATE_CLEAN = "clean";
    String JOB_TEMPLATE_SHARE = "share";

//    String JOB_SORT_BY_DEFAULT = "creation_ts_desc";
//    // Status: Warning, same value list must be defined in the res/values/arrays.xml file
//    String FILTER_BY_STATUS = "filter_by_status";
//

    // TODO rather use the JobStatus ENUM
    String JOB_STATUS_NEW = "new";
    String JOB_STATUS_PROCESSING = "processing";
    String JOB_STATUS_CANCELLING = "cancelling";
    String JOB_STATUS_PAUSING = "pausing";

    String JOB_STATUS_DONE = "done";
    String JOB_STATUS_PAUSED = "paused";
    String JOB_STATUS_WARNING = "warning";
    String JOB_STATUS_ERROR = "error";
    String JOB_STATUS_TIMEOUT = "timeout";
    // Short cut used in filtering
    String JOB_STATUS_NO_FILTER = "no_filter";

    // Specific additional status to manage uploads when remote server is not currently available
    String UPLOAD_STATUS_PRE_PROCESSING = "pre_processing";
    String UPLOAD_STATUS_LOCALLY_CACHED = "locally_cached";

    /* Transfers */
    String TRANSFER_TYPE_DOWNLOAD = "download";
    String TRANSFER_TYPE_UPLOAD = "upload";


    // Workaround to store additional destinations as state
//    String CUSTOM_PATH_ACCOUNTS = "/__acounts__";
//    String CUSTOM_PATH_BOOKMARKS = "/__bookmarks__";
//    String CUSTOM_PATH_OFFLINE = "/__offline__";
//    String CUSTOM_PATH_SHARES = "/__shares__";

    // Account Authentication States
    // TODO finalize auth state management
    String AUTH_STATUS_NEW = "new";
    String AUTH_STATUS_NO_CREDS = "no-credentials";
    String AUTH_STATUS_UNAUTHORIZED = "unauthorized";
    String AUTH_STATUS_EXPIRED = "expired";
    String AUTH_STATUS_REFRESHING = "refreshing";
    String AUTH_STATUS_CONNECTED = "connected";
    // Session Lifecycle States
    String SESSION_STATE_NEW = "new";
    String LIFECYCLE_STATE_FOREGROUND = "foreground";
    String LIFECYCLE_STATE_BACKGROUND = "background";
    String LIFECYCLE_STATE_PAUSED = "paused";
    // Offline Lifecycle States
    String OFFLINE_STATUS_NEW = "new";
    String OFFLINE_STATUS_ACTIVE = "active";
    String OFFLINE_STATUS_MIGRATED = "migrated";
    String OFFLINE_STATUS_LOST = "lost";
    String OFFLINE_STATUS_DELETED = "deleted";

    String OFFLINE_STORAGE_INTERNAL = "internal";

    // Local tree:
    // baseDir +--- cache +--- accountID +--- thumbs
    //                                   +--- previews
    //         +--- files +--- accountID +--- local
    //                                   +--- transfers
    String THUMB_PARENT_DIR = "thumbs";
    String PREVIEW_PARENT_DIR = "previews";
    String LOCAL_FILE_PARENT_DIR = "local";
    String TRANSFER_PARENT_DIR = "transfers";

    // Local types
//    String LOCAL_DIR_TYPE_CACHE = "cache";
//    String LOCAL_DIR_TYPE_FILE = "files";

    String LOCAL_FILE_TYPE_THUMB = "thumb";
    String LOCAL_FILE_TYPE_PREVIEW = "preview";
    String LOCAL_FILE_TYPE_FILE = "file";
    String LOCAL_FILE_TYPE_TRANSFER = "transfer";
    // TODO
    // String LOCAL_FILE_TYPE_EXTERNAL = "external";

    // Known log levels
    String TRACE = "Trace";
    String DEBUG = "Debug";
    String INFO = "Info";
    String WARNING = "Warning";
    String ERROR = "Error";
    String FATAL = "Fatal";
}
