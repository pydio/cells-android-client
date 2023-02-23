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
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.android.cells.ui.browse.browseNavGraph
import com.pydio.android.cells.ui.browse.screens.NoAccount
import com.pydio.android.cells.ui.core.lazyID
import com.pydio.android.cells.ui.login.LoginHost
import com.pydio.android.cells.ui.login.RouteLoginProcessAuth
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.nav.CellsDestinations
import com.pydio.android.cells.ui.nav.CellsNavigationActions
import com.pydio.android.cells.ui.system.systemNavGraph
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

private const val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    currAccountID: StateID,
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
    isExpandedScreen: Boolean,
    navController: NavHostController,
    navigateTo: (String, StateID) -> Unit,
    // openAccount: (StateID) -> Unit,
    openDrawer: () -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
) {

    val navigationActions = remember(navController) {
        CellsNavigationActions(navController)
    }

//    val alreadyDone = rememberSaveable {
//        mutableStateOf(false)
//    }
//    val preventRunning = rememberSaveable {
//        mutableStateOf(false)
//    }

    if (startingState != null) {
        Log.e(logTag, "########## Got a starting state)")
        LaunchedEffect(key1 = startingState.destination) {
            Log.e(logTag, "########## Launching side effect for ${startingState.destination})")
            Log.e(logTag, "##########     with stateID: ${startingState.stateID}")
            if (startingState.destination?.isNotEmpty() == true) {
                when {
                    startingState.destination == RouteLoginProcessAuth.route
                    -> {
                        // TODO check if startingState.state = state has already been consumed
                        navController.navigate(CellsDestinations.Login.createRoute(startingState.stateID))
                    }
                    startingState.destination!!.startsWith(CellsDestinations.Login.prefix)
                    -> {
                        // TODO When do we pass here?
                        Thread.dumpStack()
                        Log.e(logTag, "##########   Got a login destination, see above")
                        navController.navigate(CellsDestinations.Login.createRoute(startingState.stateID))
                    }
                    else -> // FIXME not sure it works
                        startingStateHasBeenProcessed(null, Transport.UNDEFINED_STATE_ID)
                }
            } else {
                startingStateHasBeenProcessed(null, Transport.UNDEFINED_STATE_ID)
            }
        }
    } else {
        LaunchedEffect(key1 = currAccountID) {
            // FIXME not good enough if we navigate to the home of the account and back here
            //  we are not redirected to the account home if we click on the **SAME** account
            if (currAccountID != Transport.UNDEFINED_STATE_ID) {
                Log.e(logTag, "########## Launching 2nd side effect for ${currAccountID})")
                navController.navigate(BrowseDestinations.Open.createRoute(currAccountID)) {
                    popUpTo(CellsDestinations.Home.route) { inclusive = true }
                }
            }
        }
    }

    // FIXME rework this for both: OAuth Callback & App launch

//        } else {
//            Log.e(logTag, "########## No defined destination, computing a new one")
//            when (currAccountID) {
//                Transport.UNDEFINED_STATE_ID ->
//                    navController.navigate(CellsDestinations.Accounts.route) {
//                        popUpTo(CellsDestinations.Home.route) { inclusive = true }
//                    }
//                else -> navController.navigate(CellsDestinations.Browse.createRoute(currAccountID))
//            }
//        }
//    }


//    val navigateTo: (String, StateID) -> Unit = { action, stateID ->
//        when (action) {
//            CellsDestinations.Login.route -> navigationActions.navigateToLogin(stateID)
//            CellsDestinations.Browse.route -> navigationActions.navigateToBrowse(stateID)
//        }
//    }

//    val login: (StateID) -> Unit = {
//        navigationActions.navigateToLogin(it)
//    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Home.route,
        route = CellsDestinations.Root.route
    ) {

        composable(CellsDestinations.Home.route) {
            NoAccount(
                openDrawer = { openDrawer() },
                addAccount = {
                    navController.navigate(
                        CellsDestinations.Login.createRoute(Transport.UNDEFINED_STATE_ID)
                    )
                },
            )
        }

        composable(CellsDestinations.Accounts.route) {
            AccountsScreen(
                currAccountID,
                navigateTo = navigateTo,
                back = { navController.popBackStack() },
                contentPadding = rememberContentPaddingForScreen(
                    additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                    excludeTop = !isExpandedScreen
                ),
            )
        }

        composable(CellsDestinations.Login.route) { bse ->
            val stateId = bse.arguments?.getString(CellsDestinations.Login.getPathKey())
                ?: Transport.UNDEFINED_STATE
            LoginHost(
                currAccount = StateID.fromId(stateId),
                // FIXME rather forward the navigateTo() method
                openAccount = { navigateTo(BrowseDestinations.Open.route, it) },
                startingState = startingState,
                startingStateHasBeenProcessed = startingStateHasBeenProcessed,
                launchIntent = launchIntent,
                back = { navController.popBackStack() },
            )
        }

        browseNavGraph(
            browseRemoteVM = browseRemoteVM,
            back = { navController.popBackStack() },
            openDrawer,
            open = {
                // TODO better handle case for parent
                //  by comparing with penultimate elements of the backStack
                Log.e(logTag, "### Opening state at $it, Backstack: ")
                val bq = navController.backQueue
                var i = 0
                navController.backQueue.forEach {
                    val stateID = lazyID(it)
                    Log.e(logTag, "#${i++} - $stateID - ${it.destination.route}")

                }
                var isEffectiveBack = false
                if (bq.size > 1) {
                    val penultimateID = lazyID(bq[bq.size - 2])
                    val second = lazyID(bq[1])
                    isEffectiveBack = penultimateID == it
                    Log.e(logTag, "### $second")
                    Log.e(logTag, "### $penultimateID <?> $it")
                }
                Log.e(logTag, "### is effective back: $isEffectiveBack")

                if (isEffectiveBack) {
                    navController.popBackStack()
                } else {
                    navController.navigate(
                        BrowseDestinations.Open.createRoute(it)
                    )
                }
            },
        )

//        composable(CellsDestinations.Browse.route) { bse ->
//            val accId = bse.arguments?.getString(CellsDestinations.Login.getPathKey())
//                ?: currAccountID.id // FIXME undefined should not be allowed here
//            val accountID = StateID.fromId(accId)
//            BrowseHost(
//                accountID = accountID,
//                openAccounts = {
//                    navigationActions.navigateToAccounts()
//                },
//                back = { navController.popBackStack() },
//                openDrawer = openDrawer
//            )
//        }

        systemNavGraph(
            isExpandedScreen,
            openDrawer,
            launchIntent = launchIntent,
            back = { navController.popBackStack() },
        )
    }
}
