package com.pydio.android.cells;

public interface AppNames {

    // TODO  make this generic
    String KEY_PREFIX = "com.pydio.android.cells";
    String KEY_PREFIX_ = KEY_PREFIX + ".";

    /* SHARED PREFERENCE KEYS */
    String PREF_KEY_INSTALLED_VERSION_CODE = "installed_version_code";
    // String PREF_KEY_CURRENT_STATE = "current_state";
    String PREF_KEY_CURR_RECYCLER_LAYOUT = "current_recycler_layout";
    String PREF_KEY_CURR_RECYCLER_ORDER = "current_recycler_order";
    // String PREF_KEY_CURR_RECYCLER_ORDER_DIR = "current_recycler_order_dir";
    String PREF_KEY_METERED_DL_THUMBS = "on_metered_dl_thumbs";
    String PREF_KEY_METERED_DL_FILES = "on_metered_dl_files";
    String PREF_KEY_OFFLINE_FREQ = "current_offline_frequency";
    String PREF_KEY_OFFLINE_CONST_WIFI = "sync_on_wifi_only";
    String PREF_KEY_OFFLINE_CONST_CHARGING = "sync_on_charging_only";

    /* SHARED PREFERENCE WELL KNOWN VALUES */
    String RECYCLER_LAYOUT_LIST = "list";
    String RECYCLER_LAYOUT_GRID = "grid";

    String DEFAULT_SORT_BY = "sort_name";
    String DEFAULT_SORT_BY_DIR = "ASC";

    String OFFLINE_FREQ_QUARTER = "quarter";
    String OFFLINE_FREQ_HOUR = "hour";
    String OFFLINE_FREQ_DAY = "day";
    String OFFLINE_FREQ_WEEK = "week";

    int ITEM_TYPE_HEADER = 0;
    int ITEM_TYPE_WS = 1;
    int ITEM_TYPE_NODE = 2;

    String SORT_BY_NAME = "name";
    String SORT_BY_MIME = "mime";
    String SORT_BY_SIZE = "size";
    String SORT_BY_REMOTE_TS = "remote_mod_ts";
    String SORT_BY_LAST_CHECK = "last_check_ts";
    String SORT_BY_DESC = "DESC";

    /* Generic actions */
    String ACTION_MORE = KEY_PREFIX_ + "more";
    String ACTION_OPEN = KEY_PREFIX_ + "open";
    String ACTION_CANCEL = KEY_PREFIX_ + "cancel";
    String ACTION_RESTART = KEY_PREFIX_ + "restart";
    String ACTION_CHOOSE_TARGET = KEY_PREFIX_ + "choosetarget";
    String ACTION_COPY = KEY_PREFIX_ + "copy";
    String ACTION_MOVE = KEY_PREFIX_ + "move";
    String ACTION_UPLOAD = KEY_PREFIX_ + "upload";

    String ACTION_OPEN_PARENT_IN_WORKSPACES = KEY_PREFIX_ + "openparentinworkspaces";
    String ACTION_DELETE_RECORD = KEY_PREFIX_ + "deleterecord";

    String ACTION_LOGIN = "login";
    String ACTION_LOGOUT = "logout";
    String ACTION_FORGET = "forget";

    /* Flags */
    int FLAG_BOOKMARK = 1;
    int FLAG_SHARE = 2;
    int FLAG_OFFLINE = 4;

    /* Modification status */
    String LOCAL_MODIF_DELETE = "deleting";
    String LOCAL_MODIF_RENAME = "renaming";
    String LOCAL_MODIF_MOVE = "moving";
    String LOCAL_MODIF_RESTORE = "restore";

    /* Transfer types */
    String TRANSFER_TYPE_DOWNLOAD = "download";
    String TRANSFER_TYPE_UPLOAD = "upload";

    /* Intents extra keys */
    String EXTRA_STATE = KEY_PREFIX_ + "state";
    String EXTRA_SERVER_URL = KEY_PREFIX_ + "server.url";
    String EXTRA_SERVER_IS_LEGACY = KEY_PREFIX_ + "server.islegacy";
    String EXTRA_AFTER_AUTH_ACTION = KEY_PREFIX_ + "auth.next.action";
    String EXTRA_ACTION_CONTEXT = KEY_PREFIX_ + "context.action";
    String EXTRA_SESSION_UID = KEY_PREFIX_ + "session.uid";

    String QUERY_KEY_CODE = "code";
    String QUERY_KEY_STATE = "state";

    String CELLS_ROOT_ENCODED_STATE = "cells%3A%2F%2Froot";

    // Workaround to store additional destinations as state
    String CUSTOM_PATH_ACCOUNTS = "/__acounts__";
    String CUSTOM_PATH_BOOKMARKS = "/__bookmarks__";
    String CUSTOM_PATH_OFFLINE = "/__offline__";
    String CUSTOM_PATH_SHARES = "/__shares__";

    // Network status
    String NETWORK_STATUS_UNKNOWN = "unknown";
    String NETWORK_STATUS_NO_INTERNET = "no_internet";
    String NETWORK_STATUS_METERED = "metered";
    String NETWORK_STATUS_OK = "ok";
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
    //                                   +--- cache
    //         +--- files +--- accountID +--- offline
    String THUMB_PARENT_DIR = "thumbs";
    String CACHED_FILE_PARENT_DIR = "cache";
    String TRANSFER_PARENT_DIR = "transfers";
    String OFFLINE_FILE_PARENT_DIR = "offline";

    // Local types
    String LOCAL_DIR_TYPE_CACHE = "cache";
    String LOCAL_DIR_TYPE_FILE = "files";
    String LOCAL_FILE_TYPE_NONE = "none";
    String LOCAL_FILE_TYPE_THUMB = "thumb";
    String LOCAL_FILE_TYPE_TRANSFER = "transfer";
    String LOCAL_FILE_TYPE_CACHE = "cache";
    String LOCAL_FILE_TYPE_OFFLINE = "offline";
    // TODO
    // String LOCAL_FILE_TYPE_EXTERNAL = "external";
}
