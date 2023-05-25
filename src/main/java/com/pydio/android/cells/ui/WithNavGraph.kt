package com.pydio.android.cells.ui

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.pydio.android.cells.ui.account.AccountListVM
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.browseNavGraph
import com.pydio.android.cells.ui.browse.composables.Download
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyQueryContext
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.LoginNavigation
import com.pydio.android.cells.ui.login.loginNavGraph
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.models.DownloadVM
import com.pydio.android.cells.ui.search.Search
import com.pydio.android.cells.ui.search.SearchHelper
import com.pydio.android.cells.ui.search.SearchVM
import com.pydio.android.cells.ui.share.ShareHelper
import com.pydio.android.cells.ui.share.shareNavGraph
import com.pydio.android.cells.ui.system.systemNavGraph
import com.pydio.cells.transport.StateID
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
    loginVM: LoginVM = koinViewModel(),
) {

    val loginNavActions = remember(navController) {
        LoginNavigation(navController)
    }

    Log.i(logTag, "### Composing nav graph for ${startingState?.route}")
    Log.i(logTag, "      isRestart: ${startingState?.isRestart ?: "false"}")

    startingState?.let {
        LaunchedEffect(key1 = it.route) {
            it.route?.let { dest ->
                // TODO double check, seems like we do not need this anymore
                navController.navigate(dest)

//                if (!it.isRestart) {
//                    Log.e(logTag, "########## Launching navigation to $dest")
//                    navController.navigate(dest)
//                } else {
//                    Thread.dumpStack()
//                    Log.e(logTag, "### Restart, preventing navigation to $dest")
//                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Accounts.route,
        route = CellsDestinations.Root.route
    ) {

        composable(CellsDestinations.Home.route) {
            NoAccount(
                openDrawer = { openDrawer() },
                addAccount = { loginNavActions.askUrl() },
            )
        }

        composable(CellsDestinations.Accounts.route) {
            val accountListVM: AccountListVM = koinViewModel()
            AccountsScreen(
                isExpandedScreen = isExpandedScreen,
                accountListVM = accountListVM,
                navigateTo = navigateTo,
                openDrawer = openDrawer,
                contentPadding = rememberContentPaddingForScreen(
                    // additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                    excludeTop = !isExpandedScreen
                ),
            )

            DisposableEffect(key1 = true) {
                accountListVM.watch()
                onDispose { accountListVM.pause() }
            }
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
            back = { navController.popBackStack() },
            openDrawer,
        )

        loginNavGraph(
            navController = navController,
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
