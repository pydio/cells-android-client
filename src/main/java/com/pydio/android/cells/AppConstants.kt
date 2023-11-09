package com.pydio.android.cells

enum class RemoteType {
    P8, CELLS
}

enum class Status {
    OK, WARNING, DANGER
}

enum class ServerConnection {
    OK, LIMITED, UNREACHABLE;

    fun isConnected(): Boolean {
        return when (this) {
            OK, LIMITED -> true
            UNREACHABLE -> false
        }
    }
}

enum class NetworkStatus {
    OK, METERED, ROAMING, CAPTIVE, UNAVAILABLE, UNKNOWN;

    fun isConnected(): Boolean {
        return when (this) {
            UNKNOWN, OK, METERED, ROAMING -> true
            UNAVAILABLE, CAPTIVE -> false
        }
    }
}

enum class LoadingState {
    // TODO we should be able to remove the server unreachable state
    STARTING, PROCESSING, IDLE, SERVER_UNREACHABLE;

    fun isRunning(): Boolean {
        return when (this) {
            STARTING, PROCESSING -> true
            else -> false
        }
    }

}

enum class SessionStatus {
    NO_INTERNET, CAPTIVE, SERVER_UNREACHABLE, NOT_LOGGED_IN, CAN_RELOG, ROAMING, METERED, OK
}

enum class LoginStatus(val id: String) {
    Undefined("undefined"),
    New("new"),
    NoCreds("no-credentials"),
    Unauthorized("unauthorized"),
    Expired("expired"),
    Refreshing("refreshing"),
    Connected("connected");

    fun isConnected(): Boolean {
        return when (this) {
            Connected -> true
            else -> false
        }
    }

    companion object {
        fun fromId(id: String): LoginStatus {
            return values().find { it.id == id }
                ?: throw IllegalArgumentException("Invalid LoginStatus id: $id")
        }
    }
}

//// TODO Finalize auth state management.
//String AUTH_STATUS_NEW = "new";
//String AUTH_STATUS_NO_CREDS = "no-credentials";
//String AUTH_STATUS_UNAUTHORIZED = "unauthorized";
//String AUTH_STATUS_EXPIRED = "expired";
//String AUTH_STATUS_REFRESHING = "refreshing";
//String AUTH_STATUS_CONNECTED = "connected";


enum class ListType {
    DEFAULT, TRANSFER, JOB
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
