package com.pydio.android.cells.ui.browse

import com.pydio.cells.transport.StateID

sealed class BrowseDestinations(val route: String) {

    //    protected val prefix = "browse/" TODO
    fun getPathKey() = "state-id"

    object Open : BrowseDestinations("open/{state-id}") {
        fun createRoute(stateID: StateID) = "open/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("open/") ?: false
    }

    object OpenCarousel : BrowseDestinations("carousel/{state-id}") {
        fun createRoute(stateID: StateID) = "carousel/${stateID.id}"
    }

    object Bookmarks : BrowseDestinations("bookmarks/{state-id}") {
        fun createRoute(stateID: StateID) = "bookmarks/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("bookmarks/") ?: false
    }

    object OfflineRoots : BrowseDestinations("offline-roots/{state-id}") {
        fun createRoute(stateID: StateID) = "offline-roots/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("offline-roots/") ?: false
    }

    object Transfers : BrowseDestinations("transfers/{state-id}") {
        fun createRoute(stateID: StateID) = "transfers/${stateID.id}"
        fun isCurrent(route: String?): Boolean = route?.startsWith("transfers/") ?: false
    }
}