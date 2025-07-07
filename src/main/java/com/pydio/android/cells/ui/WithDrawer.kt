package com.pydio.android.cells.ui

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.browse.BrowseNavigationActions
import com.pydio.android.cells.ui.core.composables.WithInternetBanner
import com.pydio.android.cells.ui.core.dumpNavigationStack
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.AppDrawer
import com.pydio.android.cells.ui.core.nav.AppPermanentDrawer
import com.pydio.android.cells.ui.core.nav.CellsNavigationActions
import com.pydio.android.cells.ui.models.AppState
import com.pydio.android.cells.ui.system.SystemNavigationActions
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val LOG_TAG = "WithDrawer.kt"

@Composable
fun NavHostWithDrawer(
    initialAppState: AppState,
    processSelectedTarget: (StateID?) -> Unit,
    emitActivityResult: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
    connectionService: ConnectionService = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val mainNavController = rememberNavController()

    val cellsNavActions = remember(mainNavController) {
        CellsNavigationActions(mainNavController)
    }
    val browseNavActions = remember(mainNavController) {
        BrowseNavigationActions(mainNavController)
    }
    val systemNavActions = remember(mainNavController) {
        SystemNavigationActions(mainNavController)
    }

    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    // Debug: understand login loop issue
    val lastRoute = rememberSaveable { mutableStateOf("") }

    val navigateTo: (String) -> Unit = { route ->
        if (route == lastRoute.value) {
            Log.w(LOG_TAG, "[WARNING] Same route called twice: $route")
        }

        // TODO remove
//        dumpNavigationStack(
//            LOG_TAG,
//            "Main NavigateTo in WithDrawer",
//            mainNavController.currentBackStack.value,
//            route
//        )

        val oldRoute = mainNavController.previousBackStackEntry?.destination?.route
        val oldState: StateID? = oldRoute?.let {
            lazyStateID(mainNavController.previousBackStackEntry, verbose = false)
        }
        val currRoute = mainNavController.currentBackStackEntry?.destination?.route
        val currState: StateID? = currRoute?.let {
            lazyStateID(mainNavController.currentBackStackEntry, verbose = false)
        }

        Log.i(LOG_TAG, "... Navigate to $route")
        Log.d(LOG_TAG, "      - Penultimate Entry route: $oldRoute, stateID: $oldState")
        Log.d(LOG_TAG, "      - Current Entry route: $currRoute, stateID: $currState")
        Log.d(LOG_TAG, "      - Local last route: ${lastRoute.value}")
        lastRoute.value = route
        coroutineScope.launch {
            mainNavController.navigate(route)
        }
    }

    val customColor = connectionService.customColor.collectAsState(null)
    val isExpandedScreen = widthSizeClass == WindowWidthSizeClass.Expanded
    val sizeAwareDrawerState = rememberSizeAwareDrawerState(isExpandedScreen)

    UseCellsTheme(
        customColor = customColor.value
    ) {
        ModalNavigationDrawer(
            drawerContent = {
                AppDrawer(
                    currRoute = navBackStackEntry?.destination?.route,
                    currSelectedID = lazyStateID(navBackStackEntry, verbose = false),
                    closeDrawer = { coroutineScope.launch { sizeAwareDrawerState.close() } },
                    connectionService = connectionService,
                    cellsNavActions = cellsNavActions,
                    systemNavActions = systemNavActions,
                    browseNavActions = browseNavActions,
                )
            },
            drawerState = sizeAwareDrawerState,
            // Only enable opening the drawer via gestures if the screen is not expanded
            gesturesEnabled = !isExpandedScreen
        ) {
            Row {
                if (isExpandedScreen) { // When we are on a tablet
                    AppPermanentDrawer(
                        currRoute = navBackStackEntry?.destination?.route,
                        currSelectedID = lazyStateID(navBackStackEntry, verbose = false),
                        connectionService = connectionService,
                        cellsNavActions = cellsNavActions,
                        systemNavActions = systemNavActions,
                        browseNavActions = browseNavActions,
                    )
                }
                WithInternetBanner(
                    connectionService = connectionService,
                    navigateTo = navigateTo,
                    contentPadding = rememberContentPaddingForScreen(
                        // additionalTop = if (!isExpandedScreen) 8.dp else 0.dp,
                        excludeTop = !isExpandedScreen
                    )
                ) {
                    CellsNavGraph(
                        initialAppState = initialAppState,
                        isExpandedScreen = isExpandedScreen,
                        navController = mainNavController,
                        navigateTo = navigateTo,
                        processSelectedTarget = processSelectedTarget,
                        emitActivityResult = emitActivityResult,
                        openDrawer = {
                            if (!isExpandedScreen) {
                                coroutineScope.launch { sizeAwareDrawerState.open() }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Determine the drawer state to pass to the modal drawer.
 */
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
