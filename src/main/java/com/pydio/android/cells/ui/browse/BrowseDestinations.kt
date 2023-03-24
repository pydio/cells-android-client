package com.pydio.android.cells.ui.browse

import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.StateID

sealed class BrowseDestinations(val route: String) {

    companion object {
        protected const val PREFIX = "browse"
    }

    object Open : BrowseDestinations("${PREFIX}/open/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    object OpenCarousel : BrowseDestinations("${PREFIX}/carousel/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/carousel/${stateID.id}"
    }

    object Bookmarks : BrowseDestinations("${PREFIX}/bookmarks/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/bookmarks/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/bookmarks/") ?: false
    }

    object OfflineRoots : BrowseDestinations("${PREFIX}/offline-roots/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/offline-roots/${stateID.id}"
        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/offline-roots/") ?: false
    }

    object Transfers : BrowseDestinations("${PREFIX}/transfers/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/transfers/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/transfers/") ?: false
    }
}