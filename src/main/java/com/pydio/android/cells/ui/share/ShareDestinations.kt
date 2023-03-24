package com.pydio.android.cells.ui.share

import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.StateID

sealed class ShareDestination(val route: String) {

    companion object {
        protected const val PREFIX = "share"
        fun isCurrent(route: String?): Boolean = route?.startsWith(PREFIX) ?: false
    }

    object ChooseAccount : ShareDestination("${PREFIX}/choose-account") {
        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    object OpenFolder : ShareDestination("${PREFIX}/open/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    object UploadInProgress :
        ShareDestination("${PREFIX}/in-progress/{${AppKeys.STATE_ID}}/{${AppKeys.UID}}") {
        fun createRoute(stateID: StateID, jobID: Long) =
            "${PREFIX}/in-progress/${stateID.id}/${jobID}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/in-progress/") ?: false
    }

    // TODO add safety checks to prevent forbidden copy-move
    //  --> to finalise we must really pass the node*s* to copy or move rather than its parent
}
