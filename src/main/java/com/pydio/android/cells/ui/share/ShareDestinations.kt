package com.pydio.android.cells.ui.share

import com.pydio.cells.transport.StateID

sealed class ShareDestination(val route: String) {

    companion object {
        protected const val KEY = "stateId"
        protected const val PREFIX = "share"
    }

    fun getPathKey(): String = KEY

    object ChooseAccount : ShareDestination("${PREFIX}/choose-account") {
        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    object OpenFolder : ShareDestination("${PREFIX}/open/{${KEY}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = "${PREFIX}/choose-account" == route
    }

    object UploadInProgress : ShareDestination("${PREFIX}/in-progress/{${KEY}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/in-progress/${stateID.id}"
    }

    // TODO add safety checks to prevent forbidden copy-move
    //  --> to finalise we must really pass the node*s* to copy or move rather than its parent
}
