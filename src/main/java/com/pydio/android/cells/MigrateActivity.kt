package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.ui.box.MigrationApp
import com.pydio.android.cells.ui.box.MigrationHost
import com.pydio.android.cells.ui.models.MigrationVM
import org.koin.androidx.viewmodel.ext.android.viewModel

class MigrateActivity : ComponentActivity() {

    private val logTag = MigrateActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching migration process")
        super.onCreate(savedInstanceState)

        val migrateActivity = this
        setContent {
            MigrationApp {
                val navController = rememberNavController()
                val migrationVM by viewModel<MigrationVM>()
                val afterMigration: () -> Unit = {
                    val intent = Intent(migrateActivity, LandActivity::class.java)
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
