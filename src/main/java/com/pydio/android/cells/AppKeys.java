package com.pydio.android.cells;

/**
 * Centralises application-wide keys.
 */
public interface AppKeys {

    /* Parameter keys for compose routes */
    String STATE_ID = "state-id";
    String STATE_IDS = "state-ids";
    String UID = "uid";
    String SKIP_VERIFY = "skip-verify";
    String LOGIN_CONTEXT = "login-context";
    String QUERY_CONTEXT = "query-context";

    /* Intent keys */
    // OAuth Code Flow
    String QUERY_KEY_CODE = "code";
    String QUERY_KEY_STATE = "state";
    // String EXTRA_STATE = AppNames.KEY_PREFIX_ + "state";

}
