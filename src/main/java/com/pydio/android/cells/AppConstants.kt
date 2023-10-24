package com.pydio.android.cells

enum class RemoteType {
    P8, CELLS
}

enum class JobStatus(val id: String) {
    NEW("new"),
    PROCESSING("processing"),
    CANCELLING("cancelling"),
    PAUSING("pausing"),
    CANCELLED("cancelled"),
    DONE("done"),
    PAUSED("paused"),
    WARNING("warning"),
    ERROR("error"),
    TIMEOUT("timeout"),
    NO_FILTER("no_filter"),
}

enum class ListContext(val id: String) {
    ACCOUNTS("accounts"),
    BROWSE("browse"),
    BOOKMARKS("bookmarks"),
    OFFLINE("offline"),
    SEARCH("search"),
    TRANSFERS("transfers"),
    SYSTEM("system"),
}

enum class LoginStatus(val id: String) {
    New("new"),
    NoCreds("no-credentials"),
    Unauthorized("unauthorized"),
    Expired("expired"),
    Refreshing("refreshing"),
    Connected("connected"),
}

enum class SessionStatus {
    NO_INTERNET, CAPTIVE, SERVER_UNREACHABLE, NOT_LOGGED_IN, CAN_RELOG, ROAMING, METERED, OK
}

enum class ListType {
    DEFAULT, TRANSFER, JOB
}
