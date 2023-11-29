package com.pydio.android.cells.ui

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.account.AccountListVM
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.browseNavGraph
import com.pydio.android.cells.ui.browse.composables.ConfirmDownloadOnLimitedConnection
import com.pydio.android.cells.ui.browse.composables.Download
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyQueryContext
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.LoginNavigation
import com.pydio.android.cells.ui.login.loginNavGraph
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.models.AppState
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.models.DownloadVM
import com.pydio.android.cells.ui.search.Search
import com.pydio.android.cells.ui.search.SearchHelper
import com.pydio.android.cells.ui.search.SearchVM
import com.pydio.android.cells.ui.share.ShareDestinations
import com.pydio.android.cells.ui.share.ShareHelper
import com.pydio.android.cells.ui.share.shareNavGraph
import com.pydio.android.cells.ui.system.systemNavGraph
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CellsNavGraph(
    initialAppState: AppState,
    isExpandedScreen: Boolean,
    navController: NavHostController,
    navigateTo: (String) -> Unit,
    openDrawer: () -> Unit,
    processSelectedTarget: (StateID?) -> Unit,
    emitActivityResult: (Int) -> Unit,
    loginVM: LoginVM = koinViewModel(),
    browseRemoteVM: BrowseRemoteVM = koinViewModel()
) {

    val logTag = "CellsNavGraph"

    val loginNavActions = remember(navController) {
        LoginNavigation(navController)
    }

    LaunchedEffect(key1 = initialAppState.intentID, key2 = initialAppState.route) {
        Log.i(logTag, "... new appState: ${initialAppState.route} - ${initialAppState.stateID}")
        initialAppState.context?.let {
            when (it) {
                AuthService.LOGIN_CONTEXT_BROWSE, AuthService.LOGIN_CONTEXT_SHARE -> {
                    // This should terminate the current task and fallback to where we were before re-launching the OAuth process
                    emitActivityResult(Activity.RESULT_OK)
                }

                AuthService.LOGIN_CONTEXT_CREATE -> {
                    navController.navigate(BrowseDestinations.Open.createRoute(initialAppState.stateID))
                }

                AuthService.LOGIN_CONTEXT_ACCOUNTS -> {
                    // Do Nothing
                }
            }
            return@LaunchedEffect
        }

        initialAppState.route?.let { dest ->
            when {
                ShareDestinations.UploadInProgress.isCurrent(dest) -> navController.navigate(dest) {
                    popUpTo(ShareDestinations.ChooseAccount.route) { inclusive = true }
                }

                else -> {
                    Log.e(logTag, "      currRoute: ${navController.currentDestination?.route}")
                    Log.e(logTag, "      newRoute: $dest")
                    navController.navigate(dest)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Accounts.route,
        route = CellsDestinations.Root.route
    ) {

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

        loginNavGraph(
            loginVM = loginVM,
            helper = LoginHelper(
                navController,
                loginVM,
                navigateTo,
            ),
        )

        browseNavGraph(
            isExpandedScreen = isExpandedScreen,
            navController = navController,
            openDrawer = openDrawer,
            back = { navController.popBackStack() },
            browseRemoteVM = browseRemoteVM,
        )

        shareNavGraph(
            isExpandedScreen = isExpandedScreen,
            browseRemoteVM = browseRemoteVM,
            helper = ShareHelper(
                navController,
                processSelectedTarget,
                emitActivityResult,
            ),
            back = { navController.popBackStack() },
        )

        systemNavGraph(
            isExpandedScreen = isExpandedScreen,
            navController = navController,
            openDrawer = openDrawer,
            back = { navController.popBackStack() },
        )

        composable(CellsDestinations.Search.route) { entry ->
            val searchVM: SearchVM =
                koinViewModel(parameters = { parametersOf(lazyStateID(entry)) })
            Search(
                isExpandedScreen = isExpandedScreen,
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
            val stateID = lazyStateID(entry)
            val canDL = remember {
                mutableStateOf(false)
            }
            val mustConfirm = remember {
                mutableStateOf(false)
            }
            val downloadVM: DownloadVM =
                koinViewModel(parameters = { parametersOf(browseRemoteVM.isLegacy, stateID) })

            LaunchedEffect(key1 = stateID) {
                Log.i(logTag, "## First Composition for: download/${stateID}")
                if (!downloadVM.mustConfirmDL(stateID)) {
                    canDL.value = true
                } else {
                    mustConfirm.value = true
                }
            }

            if (canDL.value) {
                Download(
                    stateID = stateID,
                    downloadVM = downloadVM
                ) { navController.popBackStack() }
            } else if (mustConfirm.value) {
                ConfirmDownloadOnLimitedConnection(stateID = stateID) {
                    if (it) {
                        canDL.value = true
                    } else {
                        navController.popBackStack()
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }

        composable(CellsDestinations.Home.route) {
            NoAccount(
                openDrawer = { openDrawer() },
                addAccount = { loginNavActions.askUrl() },
            )
        }
    }
}
