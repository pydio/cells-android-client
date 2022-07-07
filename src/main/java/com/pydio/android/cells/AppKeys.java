package com.pydio.android.cells;

public interface AppKeys {

    // WARNING: The migrations rely on this preference
    String INSTALLED_VERSION_CODE = "installed_version_code";

    // Manage UI
    String CURR_RECYCLER_LAYOUT = "current_recycler_layout";
    String CURR_RECYCLER_ORDER = "current_recycler_order";

    // Metered network limitations
    String APPLY_METERED_LIMITATION = "apply_metered_limitations";
    String METERED_DL_THUMBS = "on_metered_dl_thumbs";
    String METERED_ASK_B4_DL_FILES = "on_metered_ask_before_dl_files";
    String METERED_ASK_B4_DL_FILES_SIZE = "on_metered_ask_before_dl_files_greater_than";
//    String PREF_KEY_METERED_DL_FILES = "on_metered_dl_files";
    // TODO also implement finer limitations when in roaming
    // String SKIP_ROAMING_LIMITATION = "skip_roaming_limitations";

    // Offline files synchronisation worker parameters
    String SYNC_FREQ = "sync_frequency";
    String SYNC_CONST_NETWORK_TYPE = "sync_network_type";
    String SYNC_CONST_ON_CHARGING = "sync_on_charging";
    String SYNC_CONST_ON_BATT_NOT_LOW = "sync_on_batt_not_low";
    String SYNC_CONST_ON_IDLE = "sync_on_idle";


    // Monitoring
    String TRANSFER_SORT_BY = "transfer_sort_by";
    String TRANSFER_FILTER_BY_STATUS = "transfer_filter_by_status";
    String SHOW_DEBUG_TOOLS = "show_debug_tools";
    String JOB_SORT_BY = "job_sort_by";
    String JOB_FILTER_BY_STATUS = "job_filter_by_status";

    // INTENTS
    String EXTRA_STATE = AppNames.KEY_PREFIX_ + "state";
    String EXTRA_SERVER_URL = AppNames.KEY_PREFIX_ + "server.url";
    String EXTRA_SERVER_IS_LEGACY = AppNames.KEY_PREFIX_ + "server.islegacy";
    String EXTRA_AFTER_AUTH_ACTION = AppNames.KEY_PREFIX_ + "auth.next.action";
    String EXTRA_ACTION_CONTEXT = AppNames.KEY_PREFIX_ + "context.action";
    String EXTRA_SESSION_UID = AppNames.KEY_PREFIX_ + "session.uid";
}
