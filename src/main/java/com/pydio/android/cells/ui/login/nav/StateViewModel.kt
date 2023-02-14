package com.pydio.android.cells.ui.login.nav

import androidx.lifecycle.ViewModel

class StateViewModel(
    private val routeNavigator: RouteNavigator,
) : ViewModel(), RouteNavigator by routeNavigator {

    private val logTag = "LoginStateViewModel"

    fun navigateTo(route: String) {
        // Note the trick: this part of the class declaration:
        // RouteNavigator by routeNavigator
        // "Gives" all methods of a RouteNavigator to the HomeViewModel "For free"
        navigateToRoute(route)
    }

    fun navigateBack() {
        navigateUp()
    }

}
