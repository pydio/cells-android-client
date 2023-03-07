package com.pydio.android.cells.ui.login.nav

import java.util.*

/**
 * State that can be used to trigger navigation.
 */
sealed class NavigationState {

    object Idle : NavigationState()

    /**
     * @param id is used so that multiple instances of the same route will trigger multiple navigation calls.
     */
    data class NavigateToRoute(val route: String, val id: String = UUID.randomUUID().toString()) :
        NavigationState()

    /**
     * @param staticRoute is the static route to pop to, without parameter replacements.
     */
    data class PopToRoute(val staticRoute: String, val id: String = UUID.randomUUID().toString()) :
        NavigationState()

    data class NavigateUp(val id: String = UUID.randomUUID().toString()) : NavigationState()
}