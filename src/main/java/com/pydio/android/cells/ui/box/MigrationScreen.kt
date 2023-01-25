package com.pydio.android.cells.ui.box

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.box.system.AfterLegacyMigration
import com.pydio.android.cells.ui.box.system.MigrateFromV2
import com.pydio.android.cells.ui.box.system.PrepareMigration
import com.pydio.android.cells.ui.model.Migration
import com.pydio.android.cells.ui.model.Step
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.launch

// private const val logTag = "MigrationScreen.kt"

private sealed class Destinations(val route: String) {
    object PrepareMigration : Destinations("prepare-migration")
    object MigrateFromV2 : Destinations("migrate-from-v2")
    object AfterLegacyMigration : Destinations("after-legacy-migration")
    object AfterMigrationError : Destinations("after-migration-error")
}

@Composable
fun MigrationHost(
    navController: NavHostController,
    migrationVM: Migration,
    afterMigration: () -> Unit,
) {
    val ctx = LocalContext.current
    val oldVersion = migrationVM.getOldVersion(ctx)

    val currDestination = migrationVM.currDestination.collectAsState()

    LaunchedEffect(true) {
        migrationVM.migrate(ctx)
    }

    val startDestination = when (currDestination.value) {
        Step.STARTING -> Destinations.PrepareMigration.route
        Step.MIGRATING_FROM_V2 -> Destinations.MigrateFromV2.route
        Step.AFTER_LEGACY_MIGRATION -> Destinations.AfterLegacyMigration.route
        Step.AFTER_MIGRATION_ERROR -> Destinations.AfterMigrationError.route
    }

    val scope = rememberCoroutineScope()
    fun launchSync() {
        scope.launch {
            migrationVM.launchSync()
            afterMigration()
        }
    }

    /* Configure navigation */
    NavHost(
        navController = navController,
        startDestination = startDestination, // MigrationDestination.MigrateFromV2.route
    ) {

        composable(Destinations.PrepareMigration.route) {
            PrepareMigration(oldVersion)
        }

        composable(Destinations.MigrateFromV2.route) {
            val currJob = migrationVM.migrationJob.observeAsState()
            val d = (currJob.value?.progress ?: 0f).toFloat()
            val n = (currJob.value?.total ?: 1f).toFloat()
            MigrateFromV2(oldVersion, currJob.value?.progressMessage ?: "-", d, n)
        }

        composable(Destinations.AfterLegacyMigration.route) {
            AfterLegacyMigration(
                oldCodeVersion = oldVersion,
                offlineRootNb = migrationVM.rootNb,
                browse = { afterMigration() },
                launchSyncAndBrowse = { launchSync() },
            )
        }

        composable(Destinations.AfterMigrationError.route) {
            Text("Unexpected error:\n  Could not start migration process.")
        }
    }
}

@Composable
fun MigrationApp(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}
