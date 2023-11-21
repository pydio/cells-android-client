package com.pydio.android.cells.ui.share

import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.cells.transport.StateID

sealed class ShareDestination(val route: String) {

    companion object {
        protected const val PREFIX = "share"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    data object ChooseAccount : ShareDestination("${PREFIX}/choose-account") {
        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    data object OpenFolder : ShareDestination("${PREFIX}/open/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    data object UploadInProgress :
        ShareDestination("${PREFIX}/in-progress/{${AppKeys.STATE_ID}}/{${AppKeys.UID}}") {
        fun createRoute(stateID: StateID, jobID: Long) =
            "${PREFIX}/in-progress/${stateID.id}/${jobID}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/in-progress/") ?: false
    }
}
