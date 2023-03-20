package com.pydio.android.cells.ui.system

import android.content.Intent
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.pydio.android.cells.ui.core.composables.extraTopPadding
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.rememberContentPaddingForScreen
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.system.models.SettingsVM
import com.pydio.android.cells.ui.system.screens.AboutScreen
import com.pydio.android.cells.ui.system.screens.ConfirmClearCache
import com.pydio.android.cells.ui.system.screens.JobScreen
import com.pydio.android.cells.ui.system.screens.LogScreen
import com.pydio.android.cells.ui.system.screens.SettingsScreen
import com.pydio.cells.utils.Log
import org.koin.androidx.compose.koinViewModel

/**
 * App wide system and rather technical pages
 */
private const val logTag = "SystemNavGraph"

fun NavGraphBuilder.systemNavGraph(
    isExpandedScreen: Boolean,
    openDrawer: () -> Unit = {},
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    back: () -> Unit,
) {

    composable(SystemDestinations.About.route) {
        Log.d(logTag, "... Navigating to \"About\" page, expanded screen: $isExpandedScreen")
        AboutScreen(
            openDrawer = openDrawer,
            launchIntent = launchIntent,
            contentPadding = rememberContentPaddingForScreen(
                additionalTop = extraTopPadding(isExpandedScreen),
                excludeTop = !isExpandedScreen
            ),
        )
    }

    composable(SystemDestinations.Jobs.route) {
        JobScreen(openDrawer = openDrawer)
    }

    composable(SystemDestinations.Logs.route) {
        Log.d(logTag, "... About to open Logs")
        LogScreen(openDrawer = openDrawer)
    }

    dialog(SystemDestinations.ClearCache.route) { nbsEntry ->
        val stateID = lazyStateID(nbsEntry)
        val houseKeepingVM: HouseKeepingVM = koinViewModel()
        ConfirmClearCache(stateID, houseKeepingVM) {
            back()
        }
    }

    composable(SystemDestinations.Settings.route) {
        val settingsVM: SettingsVM = koinViewModel()
        SettingsScreen(
            openDrawer = openDrawer,
            settingsVM,
        )
    }
}
