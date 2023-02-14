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
import com.pydio.android.cells.ui.box.StartingState
import com.pydio.android.cells.ui.box.browse.SelectAccount
import com.pydio.android.cells.ui.login.LoginHost
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str

private val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    currAccountID: StateID,
    switchAccount: (StateID) -> Unit,
    isExpandedScreen: Boolean,
    navController: NavHostController = rememberNavController(),
    openDrawer: () -> Unit = {},
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
                startingState.destination!!.startsWith(CellsDestinations.LOGIN_ROUTE)
                -> navController.navigate(CellsDestinations.LOGIN_ROUTE)
            }
        } else {
            Log.e(logTag, "########## No defined destination, computing a new one")
            when (currAccountID) {
                Transport.UNDEFINED_STATE_ID ->
                    navController.navigate(CellsDestinations.ACCOUNTS_ROUTE) {
                        popUpTo(CellsDestinations.HOME_ROUTE) { inclusive = true }
                    }
                else -> navController.navigate(CellsDestinations.BROWSE_ROUTE)
            }
        }
    }

    val login: (StateID) -> Unit = {
        navigationActions.navigateToLogin(it)
    }

    NavHost(
        navController = navController,
        startDestination = CellsDestinations.HOME_ROUTE,
        // startDestination = startDestination,
        route = CellsDestinations.ROOT_ROUTE
    ) {

        composable(CellsDestinations.HOME_ROUTE) {
            Column {

                Button(onClick = openDrawer) {
                    Text("Open Drawer")
                }
                Text("Home")
            }
        }

        composable(CellsDestinations.BROWSE_ROUTE) {
            Text("Browse")
        }

        composable(CellsDestinations.LOGIN_ROUTE) {
            LoginHost(
                startingState = startingState,
                launchIntent = launchIntent,
                back = { navController.popBackStack() },
            )
        }

        composable(CellsDestinations.ACCOUNTS_ROUTE) {
            Log.e(logTag, "... Will navigate to accounts, expanded screen: $isExpandedScreen")

            SelectAccount(
                currAccountID,
                switchAccount = switchAccount,
                login = login,
                back = { navController.popBackStack() },
                contentPadding = rememberContentPaddingForScreen(
                    additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                    excludeTop = !isExpandedScreen
                ),
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
