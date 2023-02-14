package com.pydio.android.cells.ui.nav

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.login.LoginHost

private val logTag = "CellsNavGraph"

@Composable
fun CellsNavGraph(
    isExpandedScreen: Boolean,
    navController: NavHostController = rememberNavController(),
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    openDrawer: () -> Unit = {},
    // startDestination: String = CellsDestinations.LOGIN_ROUTE,
    startDestination: String = CellsDestinations.HOME_ROUTE,
) {

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
                launchIntent = launchIntent,
                back = { navController.popBackStack() },
            )
        }

        systemNavGraph(
            isExpandedScreen,
            openDrawer,
            launchIntent = launchIntent,
            back = { navController.popBackStack() },
        )

//        composable(CellsDestinations.ABOUT_ROUTE) {
//            Log.e(logTag, "... Will navigate to About, expanded screen: $isExpandedScreen")
//            AboutScreen(
//                openDrawer = openDrawer,
//                launchIntent = launchIntent,
//                contentPadding = rememberContentPaddingForScreen(
//                    additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
//                    excludeTop = !isExpandedScreen
//                ),
//            )
//        }
    }
}

