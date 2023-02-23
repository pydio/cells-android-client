package com.pydio.android.cells.ui

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.core.composables.WithInternetBanner
import com.pydio.android.cells.ui.nav.AppDrawer
import com.pydio.android.cells.ui.nav.AppNavRail
import com.pydio.android.cells.ui.nav.CellsDestinations
import com.pydio.android.cells.ui.nav.CellsNavigationActions
import com.pydio.android.cells.ui.nav.SystemNavigationActions
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavHostWithDrawer(
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
//    currAccountID: StateID,
//    openAccount: (StateID) -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    connectionVM: ConnectionVM = koinViewModel(),
) {

    val isExpandedScreen = widthSizeClass == WindowWidthSizeClass.Expanded
    val sizeAwareDrawerState = rememberSizeAwareDrawerState(isExpandedScreen)

    val coroutineScope = rememberCoroutineScope()

    val navHostController = rememberNavController()
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()

    val activeSessionView = connectionVM.sessionView.observeAsState()

//
//    val openAccount: (StateID) -> Unit = {
//        currAccountID.value = it
//        Log.e(logTag, "--- Open Account: $it")
//    }


    val navigationActions = remember(navHostController) {
        CellsNavigationActions(navHostController)
    }
    val systemActions = remember(navHostController) {
        SystemNavigationActions(navHostController)
    }

    val navigateTo: (String, StateID) -> Unit = { action, stateID ->
        when (action) {
            CellsDestinations.Login.route -> navigationActions.navigateToLogin(stateID)
            BrowseDestinations.Open.route -> navigationActions.navigateToBrowse(stateID)
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            AppDrawer(
                currentRoute = navBackStackEntry?.destination?.route,
                connectionVM = connectionVM,
                cellsNavigationActions = navigationActions,
                systemNavigationActions = systemActions,
                navigateToBrowse = { navigationActions.navigateToBrowse(it) },
                closeDrawer = { coroutineScope.launch { sizeAwareDrawerState.close() } },
            )
        },
        drawerState = sizeAwareDrawerState,
        // Only enable opening the drawer via gestures if the screen is not expanded
        gesturesEnabled = !isExpandedScreen
    ) {
        Row {
            if (isExpandedScreen) { // When we are on a tablet
                // FIXME this is only partially implemented
                AppNavRail(
                    currentRoute = navBackStackEntry?.destination?.route,
                    navigateToHome = navigationActions.navigateToHome,
                    navigateToAbout = systemActions.navigateToAbout,
                )
            }
            WithInternetBanner(
                connectionVM = connectionVM,
                navigateTo = navigateTo,
                contentPadding = rememberContentPaddingForScreen(
                    additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                    excludeTop = !isExpandedScreen
                )
            ) {
                CellsNavGraph(
                    currAccountID = activeSessionView.value?.getStateID()
                        ?: Transport.UNDEFINED_STATE_ID,
                    startingState = startingState,
                    startingStateHasBeenProcessed = startingStateHasBeenProcessed,
                    isExpandedScreen = isExpandedScreen,
                    navController = navHostController,
                    navigateTo = navigateTo,
                    openDrawer = { coroutineScope.launch { sizeAwareDrawerState.open() } },
                    launchIntent = launchIntent,
                )
            }
        }
    }
}

/**
 * Determine the drawer state to pass to the modal drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberSizeAwareDrawerState(isExpandedScreen: Boolean): DrawerState {
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    return if (!isExpandedScreen) {
        // If we want to allow showing the drawer, we use a real, remembered drawer
        // state defined above
        drawerState
    } else {
        // If we don't want to allow the drawer to be shown, we provide a drawer state
        // that is locked closed. This is intentionally not remembered, because we
        // don't want to keep track of any changes and always keep it closed
        DrawerState(DrawerValue.Closed)
    }
}

/**
 * Determine the content padding to apply to the different screens of the app
 */
@Composable
fun rememberContentPaddingForScreen(
    additionalTop: Dp = 0.dp,
    excludeTop: Boolean = false
) =
    WindowInsets.systemBars
        .only(if (excludeTop) WindowInsetsSides.Bottom else WindowInsetsSides.Vertical)
        .add(WindowInsets(top = additionalTop))
        .asPaddingValues()