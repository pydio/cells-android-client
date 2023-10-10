package com.pydio.android.cells.ui.browse

import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.core.encodeStateForRoute
import com.pydio.cells.transport.StateID

sealed class BrowseDestinations(val route: String) {

    companion object {
        protected const val logTag = "BrowseDestinations"
        protected const val PREFIX = "browse"
    }

    data object Open : BrowseDestinations("${PREFIX}/open/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID): String {
            return "${PREFIX}/open/${encodeStateForRoute(stateID)}"

        }

        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/open/") ?: false
    }

    data object OpenCarousel : BrowseDestinations("${PREFIX}/carousel/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/carousel/${encodeStateForRoute(stateID)}"
    }

    data object Bookmarks : BrowseDestinations("${PREFIX}/bookmarks/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/bookmarks/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/bookmarks/") ?: false
    }

    data object OfflineRoots : BrowseDestinations("${PREFIX}/offline-roots/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) =
            "${PREFIX}/offline-roots/${encodeStateForRoute(stateID)}"

        fun isCurrent(route: String?): Boolean =
            route?.startsWith("${PREFIX}/offline-roots/") ?: false
    }

    data object Transfers : BrowseDestinations("${PREFIX}/transfers/{${AppKeys.STATE_ID}}") {
        fun createRoute(stateID: StateID) = "${PREFIX}/transfers/${encodeStateForRoute(stateID)}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("${PREFIX}/transfers/") ?: false
    }
}