package com.pydio.android.cells.ui.system

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import com.pydio.android.cells.ui.core.composables.extraTopPadding
import com.pydio.android.cells.ui.core.lazyStateID
import com.pydio.android.cells.ui.system.models.JobListVM
import com.pydio.android.cells.ui.system.models.LogListVM
import com.pydio.android.cells.ui.rememberContentPaddingForScreen
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.system.models.SettingsVM
import com.pydio.android.cells.ui.system.screens.AboutScreen
import com.pydio.android.cells.ui.system.screens.ConfirmClearCache
import com.pydio.android.cells.ui.system.screens.JobScreen
import com.pydio.android.cells.ui.system.screens.LogScreen
import com.pydio.android.cells.ui.system.screens.PreferencesScreen
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
        Log.e(logTag, "... Will navigate to About, expanded screen: $isExpandedScreen")
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
        val jobVM: JobListVM = koinViewModel()
        val jobs by jobVM.jobs.observeAsState()
        JobScreen(
            jobs = jobs ?: listOf(),
            openDrawer = openDrawer,
        )
    }

    composable(SystemDestinations.Logs.route) {
        val logVM: LogListVM = koinViewModel()
        val logs by logVM.logs.observeAsState()
        LogScreen(
            logs = logs ?: listOf(),
            openDrawer = openDrawer,
        )
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
        PreferencesScreen(
            openDrawer = openDrawer,
            settingsVM,
        )
    }
}
