package com.pydio.android.cells.ui.nav

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.core.composables.extraTopPadding
import com.pydio.android.cells.ui.system.screens.AboutScreen
import com.pydio.android.cells.ui.system.screens.JobScreen
import com.pydio.android.cells.ui.system.screens.LogScreen
import com.pydio.android.cells.ui.models.JobListVM
import com.pydio.android.cells.ui.models.LogListVM
import com.pydio.android.cells.ui.rememberContentPaddingForScreen
import com.pydio.android.cells.ui.system.screens.TransferScreen
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

    composable(SystemDestinations.ABOUT_ROUTE) {
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

    composable(SystemDestinations.JOBS_ROUTE) {
        val jobVM: JobListVM = koinViewModel()
        val jobs by jobVM.jobs.observeAsState()
        JobScreen(
            jobs = jobs ?: listOf(),
            openDrawer = openDrawer,
        )
    }

    composable(SystemDestinations.LOGS_ROUTE) {
        val logVM: LogListVM = koinViewModel()
        val logs by logVM.logs.observeAsState()
        LogScreen(
            logs = logs ?: listOf(),
            openDrawer = openDrawer,
        )
    }

    composable(SystemDestinations.TRANSFERS_ROUTE) {
//        val jobVM: JobListVM = koinViewModel()
//        val jobs by jobVM.jobs.observeAsState()
//        TransferScreen(jobs ?: listOf())
    }

    composable(SystemDestinations.CLEAR_CACHE_ROUTE) {
        Text("Implement me")
    }

}
