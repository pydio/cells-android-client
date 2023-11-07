package com.pydio.android.cells.ui.system

import android.content.Intent
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.system.screens.AboutScreen
import com.pydio.android.cells.ui.system.screens.HouseKeeping
import com.pydio.android.cells.ui.system.screens.JobScreen
import com.pydio.android.cells.ui.system.screens.LogScreen
import com.pydio.android.cells.ui.system.screens.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * App-wide system and technical pages.
 */
fun NavGraphBuilder.systemNavGraph(
    isExpandedScreen: Boolean,
    navController: NavHostController,
    openDrawer: () -> Unit = {},
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    back: () -> Unit,
) {

    // val logTag = "SystemNavGraph"

    composable(SystemDestinations.About.route) {
        AboutScreen(
            isExpandedScreen = isExpandedScreen,
            openDrawer = openDrawer,
            launchIntent = launchIntent,
        )
    }

    composable(SystemDestinations.Jobs.route) {
        JobScreen(openDrawer = openDrawer)
    }

    composable(SystemDestinations.Logs.route) {
        LogScreen(openDrawer = openDrawer)
    }

    composable(SystemDestinations.ClearCache.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val houseKeepingVM: HouseKeepingVM = koinViewModel(parameters = { parametersOf(stateID) })
        HouseKeeping(
            isExpandedScreen = isExpandedScreen,
            houseKeepingVM = houseKeepingVM,
            openDrawer = openDrawer,
            dismiss = {
                if (it) {
                    navController.navigate(CellsDestinations.Accounts.route)
                } else {
                    back()
                }
            },
        )
    }

    composable(SystemDestinations.Settings.route) {
        SettingsScreen(
            isExpandedScreen = isExpandedScreen,
            openDrawer = openDrawer,
        )
    }
}
