package com.pydio.android.cells.ui

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.browseNavGraph
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyQueryContext
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.LoginNavigation
import com.pydio.android.cells.ui.login.loginNavGraph
import com.pydio.android.cells.ui.login.models.NewLoginVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.search.Search
import com.pydio.android.cells.ui.search.SearchHelper
import com.pydio.android.cells.ui.search.SearchVM
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.android.cells.ui.share.ShareHelper
import com.pydio.android.cells.ui.share.shareNavGraph
import com.pydio.android.cells.ui.system.systemNavGraph
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    currAccountID: StateID,
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
    isExpandedScreen: Boolean,
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    openDrawer: () -> Unit,
    launchTaskFor: (String, StateID) -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
    loginVM: NewLoginVM = koinViewModel(),
) {

    val scope = rememberCoroutineScope()
    val loginNavActions = remember(navController) {
        LoginNavigation(navController)
    }

    // Starting state is null once the initial state has been consumed
    if (startingState != null) {
        Log.i(logTag, "########## Got a starting state")
        LaunchedEffect(key1 = startingState.route) {
            Log.e(logTag, "########## Launching side effect for ${startingState.route}")
            Log.e(logTag, "##########     with stateID: ${startingState.stateID}")
            if (startingState.route?.isNotEmpty() == true) {

                when {
                    LoginDestinations.ProcessAuth.isCurrent(startingState.route)
                    -> {
                        // TODO check if we need to be aware of the skip verify flag at this point
                        loginNavActions.processAuth(startingState.stateID, false)
                    }

                    LoginDestinations.isCurrent(startingState.route)
                    -> {
                        // TODO When do we pass here?
                        Thread.dumpStack()
                        Log.e(logTag, "##########   Got a login destination with route:")
                        Log.e(logTag, "##########    ${startingState.route}, see above")
                        loginNavActions.askUrl()
                    }

                    ShareDestination.ChooseAccount.isCurrent(startingState.route)
                    -> navController.navigate(ShareDestination.ChooseAccount.route)

                    Str.notEmpty(startingState.route)
                    -> {
                        // TODO should be the normal behaviour for starting state
                        //   normally we can rely on the route if present => that's the goal
                        navController.navigate(startingState.route!!)
                    }

                    else  // Nothing special to do, we remove the starting state
                    -> startingStateHasBeenProcessed(null, StateID.NONE)
                }
            } else { // Same as 2 lines above, but should never happen
                Thread.dumpStack()
                Log.e(logTag, "## Starting state for ${startingState.stateID} with no route")
                startingStateHasBeenProcessed(null, StateID.NONE)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Home.route,
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

        browseNavGraph(
            // Temporary FIXME remove
            navController = navController,
            browseRemoteVM = browseRemoteVM,
            back = { navController.popBackStack() },
            openDrawer,
            open = {
                Log.e(logTag, "### Calling open for $it")

                // Kind of tweak: we check if the target node is the penultimate
                // element of the backStack, in such case we consider it is a back:
                // the end user has clicked on parent() and was "simply" browsing
                // Log.d(logTag, "### Opening state at $it, Backstack: ")
                val bq = navController.backQueue
                // var i = 0
                // navController.backQueue.forEach {
                //     val stateID = lazyStateID(it)
                //     Log.e(logTag, "#${i++} - $stateID - ${it.destination.route}")

                // }
                var isEffectiveBack = false
                if (bq.size > 1) {
                    val targetEntry = bq[bq.size - 2]
                    val penultimateID = lazyStateID(bq[bq.size - 2])
                    isEffectiveBack =
                        BrowseDestinations.Open.isCurrent(targetEntry.destination.route)
                                && penultimateID == it && it != StateID.NONE
                }
                if (isEffectiveBack) {
                    Log.e(logTag, "Open node at $it is Effective Back")
                    navController.popBackStack()
                } else {
                    scope.launch {
                        val route : String
                        if (Str.notEmpty(it.workspace)) {
                            val item = browseRemoteVM.getTreeNode(it) ?: run {
                                // We cannot navigate to an unknown node item
                                Log.e(logTag, "No TreeNode found for $it in local repo, aborting")
                                return@launch
                            }
                            if (item.isFolder()) {
                                route = BrowseDestinations.Open.createRoute(it)
                            } else if (item.isPreViewable()) {
                                route = BrowseDestinations.OpenCarousel.createRoute(it)
                            } else {
                                // FIXME launch external view
                                Log.e(
                                    logTag,
                                    "Implement me - not a viewable file for $it, aborting"
                                )
                                return@launch
                            }
                        } else if (it == StateID.NONE) {
                            route = CellsDestinations.Accounts.route
                        } else {
                            route = BrowseDestinations.Open.createRoute(it)
                        }
                        navController.navigate(route)
                    }
                }
            },
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
