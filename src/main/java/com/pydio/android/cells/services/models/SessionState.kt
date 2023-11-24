package com.pydio.android.cells.services.models

import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.cells.transport.StateID

data class SessionState(
    val accountID: StateID,
    val isServerReachable: Boolean,
    val networkStatus: NetworkStatus,
    val loginStatus: LoginStatus,
    val isServerLegacy: Boolean = false
) {
    companion object {
        fun from(view: RSessionView, status: NetworkStatus): SessionState {
            return SessionState(
                accountID = view.getStateID(),
                isServerReachable = view.isReachable,
                networkStatus = status,
                loginStatus = LoginStatus.fromId(view.authStatus),
                isServerLegacy = view.isLegacy
            )
        }
    }
}

fun SessionState.isOK(): Boolean {
    // TODO rather also rely on server connection to also take prefs limit in account
    return isServerReachable && networkStatus == NetworkStatus.OK && loginStatus.isConnected()
}
