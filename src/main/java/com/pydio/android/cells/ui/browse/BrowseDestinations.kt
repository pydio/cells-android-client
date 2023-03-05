package com.pydio.android.cells.ui.browse

import com.pydio.cells.transport.StateID

sealed class BrowseDestinations(val route: String) {

    companion object {
        protected const val STATE_ID_KEY = "state-id"
        protected const val UID_KEY = "uid"
        protected const val PREFIX = "browse"
    }

    fun getStateIdKey(): String = STATE_ID_KEY
    fun getUidKey(): String = UID_KEY

    object Open : BrowseDestinations("${PREFIX}/open/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    object OpenCarousel : BrowseDestinations("${PREFIX}/carousel/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/carousel/${stateID.id}"
    }

    object Bookmarks : BrowseDestinations("${PREFIX}/bookmarks/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/bookmarks/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/bookmarks/") ?: false
    }

    object OfflineRoots : BrowseDestinations("${PREFIX}/offline-roots/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/offline-roots/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/offline-roots/") ?: false
    }

    object Transfers : BrowseDestinations("${PREFIX}/transfers/{$STATE_ID_KEY}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/transfers/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/transfers/") ?: false
    }
}