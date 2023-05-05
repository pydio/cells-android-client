package com.pydio.android.cells;

/**
 * Centralises application-wide keys.
 */
public interface AppKeys {

    // Extra parameters for compose routes
    String STATE_ID = "state-id";
    String UID = "uid";
    String QUERY_CONTEXT = "query-context";
    String SKIP_VERIFY = "skip-verify";

    // Intent keys
    String EXTRA_STATE = AppNames.KEY_PREFIX_ + "state";
}
