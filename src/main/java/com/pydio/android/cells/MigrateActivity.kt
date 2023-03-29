package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.migration.MigrationHost
import com.pydio.android.cells.ui.migration.MigrationVM
import com.pydio.android.cells.ui.theme.CellsTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 *  We use a distinct activity for migration so that we are sure that
 *  the legacy repositories and objects are not loaded we are on the "happy path" a.k.a
 *  when no migration is required
 */
class MigrateActivity : ComponentActivity() {

    private val logTag = "MigrateActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching migration process")
        super.onCreate(savedInstanceState)

        val migrateActivity = this
        setContent {
            CellsTheme {
                val navController = rememberNavController()
                val migrationVM by viewModel<MigrationVM>()
                val afterMigration: () -> Unit = {
                    val intent = Intent(migrateActivity, MainActivity::class.java)
                    intent.action = Intent.ACTION_MAIN
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    startActivity(intent)
                    migrateActivity.finish()
                }

                MigrationHost(
                    navController = navController,
                    migrationVM = migrationVM,
                    afterMigration
                )
            }
        }
    }
}
