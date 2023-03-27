package com.pydio.android.cells.ui

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.browseNavGraph
import com.pydio.android.cells.ui.browse.composables.Download
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyQueryContext
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.LoginNavigation
import com.pydio.android.cells.ui.login.loginNavGraph
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.DownloadVM
import com.pydio.android.cells.ui.search.Search
import com.pydio.android.cells.ui.search.SearchHelper
import com.pydio.android.cells.ui.search.SearchVM
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.android.cells.ui.share.ShareHelper
import com.pydio.android.cells.ui.share.shareNavGraph
import com.pydio.android.cells.ui.system.systemNavGraph
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
    isExpandedScreen: Boolean,
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    openDrawer: () -> Unit,
    launchTaskFor: (String, StateID) -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
    loginVM: LoginVM = koinViewModel(),
) {

    val loginNavActions = remember(navController) {
        LoginNavigation(navController)
    }

    // Starting state is null once the initial state has been consumed
    if (startingState != null) {
        LaunchedEffect(key1 = startingState.route) {
            Log.e(logTag, "########## Launching side effect for ${startingState.route}")
            Log.e(logTag, "##########     with stateID: ${startingState.stateID}")
            if (startingState.route?.isNotEmpty() == true) {

                when {
                    // First we explicitly handle the well known routes for future debugging
                    // OAuth Call back
                    LoginDestinations.ProcessAuth.isCurrent(startingState.route)
                    -> loginNavActions.processAuth(startingState.stateID, false)
                    // Until we define at least one account
                    LoginDestinations.AskUrl.isCurrent(startingState.route)
                    -> loginNavActions.askUrl()
                    // Share with Pydio from another app
                    ShareDestination.ChooseAccount.isCurrent(startingState.route)
                    -> navController.navigate(ShareDestination.ChooseAccount.route)

                    // Failsafe that are still to be investigated
                    LoginDestinations.isCurrent(startingState.route)
                    -> {
                        // TODO When do we pass here?
                        Thread.dumpStack()
                        Log.e(logTag, "##########   Got a login destination with route:")
                        Log.e(logTag, "##########    ${startingState.route}, see above")
                        loginNavActions.askUrl()
                    }

                    Str.notEmpty(startingState.route)
                    -> {
                        // TODO should be the normal behaviour for starting state
                        //   normally we can rely on the route if present => that's the goal
                        navController.navigate(startingState.route!!)
                    }

                    else  // Nothing special to do, we remove the starting state
                    -> startingStateHasBeenProcessed(null, StateID.NONE)
                }
            } else { // Same as 2 lines above, but this should never happen
                Thread.dumpStack()
                Log.e(logTag, "## Starting state for ${startingState.stateID} with no route")
                startingStateHasBeenProcessed(null, StateID.NONE)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Accounts.route,
        // route = CellsDestinations.Home.route
        route = CellsDestinations.Root.route
    ) {

        composable(CellsDestinations.Home.route) {
            NoAccount(
                openDrawer = { openDrawer() },
                addAccount = {
                    loginNavActions.askUrl()
                },
            )
        }

        composable(CellsDestinations.Accounts.route) {
            AccountsScreen(
                navigateTo = navigateTo,
                openDrawer = openDrawer,
                contentPadding = rememberContentPaddingForScreen(
                    additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                    excludeTop = !isExpandedScreen
                ),
            )
        }

        composable(CellsDestinations.Search.route) { entry ->
            val searchVM: SearchVM =
                koinViewModel(parameters = { parametersOf(lazyStateID(entry)) })
            Search(
                queryContext = lazyQueryContext(entry),
                stateID = lazyStateID(entry),
                searchVM = searchVM,
                SearchHelper(
                    navController = navController,
                    searchVM = searchVM
                ),
            )
        }

        dialog(CellsDestinations.Download.route) { entry ->
            val downloadVM: DownloadVM =
                koinViewModel(parameters = { parametersOf(lazyStateID(entry)) })
            Download(
                stateID = lazyStateID(entry),
                downloadVM = downloadVM,
                dismiss = { navController.popBackStack() }
            )
        }

        browseNavGraph(
            navController = navController,
            browseRemoteVM = browseRemoteVM,
            back = { navController.popBackStack() },
            openDrawer,
        )

        loginNavGraph(
            loginVM = loginVM,
            helper = LoginHelper(
                navController,
                loginVM,
                navigateTo,
                startingState,
                startingStateHasBeenProcessed
            ),
        )

        shareNavGraph(
            browseRemoteVM = browseRemoteVM,
            helper = ShareHelper(
                navController,
                launchTaskFor,
                startingState,
                startingStateHasBeenProcessed
            ),
            back = { navController.popBackStack() },
        )

        systemNavGraph(
            isExpandedScreen,
            openDrawer,
            launchIntent = launchIntent,
            back = { navController.popBackStack() },
        )
    }
}
