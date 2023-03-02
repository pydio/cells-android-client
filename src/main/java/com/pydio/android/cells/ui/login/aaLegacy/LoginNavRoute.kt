package com.pydio.android.cells.ui.login.aaLegacy

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.login.nav.NavigationState
import com.pydio.android.cells.ui.login.nav.RouteNavigator

/**
 * Navigation Route adapter for login pages:
 * So that we can hoist some of the data in the LoginNavHost.
 *
 * This is forked from https://github.com/Frank1234/ViewModelNavigationCompose
 * to test navigation with state from a view model.
 * Not yet completely satisfying.
 */
interface LoginNavRoute<T : RouteNavigator> {

    val route: String

    @Composable
    fun Content(
        viewModel: T,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit,
    )

    @Composable
    fun viewModel(): T

    /**
     * Override when this page uses arguments.
     * We do it here and not in the [NavigationComponent to keep it centralized]
     */
    fun getArguments(): List<NamedNavArgument> = listOf()

    /**
     * Generates the composable for this route.
     */
    fun composable(
        builder: NavGraphBuilder,
        navHostController: NavHostController,
        loginVM: LoginViewModelNew,
        navigateTo: (String?) -> Unit,
    ) {
        builder.composable(route, getArguments()) {
            val viewModel = viewModel()
            val viewStateAsState by viewModel.navigationState.collectAsState()

            LaunchedEffect(viewStateAsState) {
                Log.e("Nav", "${this@LoginNavRoute} updateNavigationState to $viewStateAsState")
                updateNavigationState(navHostController, viewStateAsState, viewModel::onNavigated)
            }

            Content(viewModel, loginVM, navigateTo)
        }
    }

    /**
     * Navigates to viewState.
     */
    private fun updateNavigationState(
        navHostController: NavHostController,
        navigationState: NavigationState,
        onNavigated: (navState: NavigationState) -> Unit,
    ) {
        when (navigationState) {
            is NavigationState.NavigateToRoute -> {
                navHostController.navigate(navigationState.route)
                onNavigated(navigationState)
            }
            is NavigationState.PopToRoute -> {
                navHostController.popBackStack(navigationState.staticRoute, false)
                onNavigated(navigationState)
            }
            is NavigationState.NavigateUp -> {
                navHostController.navigateUp()
            }
            is NavigationState.Idle -> {
            }
        }
    }
}
