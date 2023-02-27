package com.pydio.android.cells.ui.share

import com.pydio.cells.transport.StateID

sealed class ShareDestination(val route: String) {

    companion object {
        protected const val STATE_ID_KEY = "state-id"
        protected const val UID_KEY = "uid"
        protected const val PREFIX = "share"
    }

    fun getStateIdKey(): String = STATE_ID_KEY
    fun getUidKey(): String = UID_KEY

    object ChooseAccount : ShareDestination("${PREFIX}/choose-account") {
        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    object OpenFolder : ShareDestination("${PREFIX}/open/{${STATE_ID_KEY}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    object UploadInProgress : ShareDestination("${PREFIX}/in-progress/{${STATE_ID_KEY}}/{${UID_KEY}}") {
        fun createRoute(stateID: StateID, jobID: Long) = "${PREFIX}/in-progress/${stateID.id}/${jobID}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/in-progress/") ?: false
    }

    // TODO add safety checks to prevent forbidden copy-move
    //  --> to finalise we must really pass the node*s* to copy or move rather than its parent
}
