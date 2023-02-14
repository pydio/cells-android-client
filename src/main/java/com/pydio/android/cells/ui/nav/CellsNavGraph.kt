package com.pydio.android.cells.ui.nav

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.account.AccountsScreen
import com.pydio.android.cells.ui.browse.BrowseHost
import com.pydio.android.cells.ui.login.LoginHost
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str

private const val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    currAccountID: StateID,
    openAccount: (StateID) -> Unit,
    isExpandedScreen: Boolean,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    startingState: StartingState,
) {

//    if (startingState.destination?.startsWith(CellsDestinations.LOGIN_ROUTE) == true) {
//        startingState.destination!!
//    } else {
//        ?: CellsDestinations.HOME_ROUTE
//    }

    val navigationActions = remember(navController) {
        CellsNavigationActions(navController)
    }

    LaunchedEffect(key1 = startingState) {
        Log.e(logTag, "########## Launching side effect for ${startingState.destination})")
        Log.e(logTag, "##########     with stateID: ${startingState.stateID}")
        if (Str.notEmpty(startingState.destination)) {
            when {
                startingState.destination!!.startsWith(CellsDestinations.Login.prefix)
                -> navController.navigate(CellsDestinations.Login.createRoute(startingState.stateID))
            }
        } else {
            Log.e(logTag, "########## No defined destination, computing a new one")
            when (currAccountID) {
                Transport.UNDEFINED_STATE_ID ->
                    navController.navigate(CellsDestinations.Accounts.route) {
                        popUpTo(CellsDestinations.Home.route) { inclusive = true }
                    }
                else -> navController.navigate(CellsDestinations.Browse.createRoute(currAccountID))
            }
        }
    }

    LaunchedEffect(key1 = currAccountID) {
        // FIXME not good enough if we navigate to the home of the account and back here
        //  we are not redirected to the account home if we click on the **SAME** account
        Log.e(logTag, "########## Launching side effect 222 for ${currAccountID})")
        if (currAccountID != Transport.UNDEFINED_STATE_ID) {
            navController.navigate(CellsDestinations.Browse.createRoute(currAccountID)) {
                popUpTo(CellsDestinations.Home.route) { inclusive = true }
            }
        }
    }

    val login: (StateID) -> Unit = {
        navigationActions.navigateToLogin(it)
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.Home.route,
        route = CellsDestinations.Root.route
    ) {

        composable(CellsDestinations.Home.route) {
            Column {
                Button(onClick = openDrawer) {
                    Text("Open Drawer")
                }
                Text("Home")
            }
        }

        composable(CellsDestinations.Accounts.route) {
            AccountsScreen(
                currAccountID,
                switchAccount = openAccount,
                login = login,
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
                openAccount = openAccount,
                startingState = startingState,
                launchIntent = launchIntent,
                back = { navController.popBackStack() },
            )
        }

        composable(CellsDestinations.Browse.route) { bse ->
            val accId = bse.arguments?.getString(CellsDestinations.Login.getPathKey())
                ?: currAccountID.id // FIXME undefined should not be allowed here
            val accountID = StateID.fromId(accId)
            BrowseHost(
                accountID = accountID,
                openAccounts = { navigationActions.navigateToAccounts },
                back = { navController.popBackStack() },
                openDrawer = openDrawer
            )
        }

        systemNavGraph(
            isExpandedScreen,
            openDrawer,
            launchIntent = launchIntent,
            back = { navController.popBackStack() },
        )
    }
}
