package com.pydio.android.cells

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.databinding.ActivityLandBinding
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.legacy.v2.MainDB
import com.pydio.android.legacy.v2.MigrationServiceV2
import com.pydio.android.legacy.v2.SyncDB
import com.pydio.cells.api.SdkNames
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class LandActivity : AppCompatActivity() {

    private val logTag = LandActivity::class.simpleName
    private val accountService: AccountService by inject()
    private val accountDao: AccountDao by inject()
    private val prefs: CellsPreferences by inject()

    private lateinit var binding: ActivityLandBinding

    private val migrationService = MigrationServiceV2()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_land)

        if (intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true) {
            lifecycleScope.launch {
                if (needsMigration(applicationContext)) {
                    binding.migrationReportPanel.visibility = View.VISIBLE
                    withContext(Dispatchers.IO) {
                        migrate(applicationContext)
                    }
                } else {
                    chooseFirstPage()
                }
            }
        }
    }

    private suspend fun chooseFirstPage() {
        val landActivity = this

        var stateID: StateID? = null
        // Fallback on defined accounts:
        val accounts = withContext(Dispatchers.IO) { accountDao.getAccounts() }
        when (accounts.size) {
            0 -> { // No account: launch registration
                startActivity(Intent(landActivity, AuthActivity::class.java))
                landActivity.finish()
                return
            }
            1 -> { // Only one: force state to its root
                stateID = StateID.fromId(accounts[0].accountID)
            }
            // else we navigate to the MainActivity with no state,
            //  that should led us to the account list
            //  size > 1 -> navController.navigate(MainNavDirections.openAccountList())
        }
        val intent = Intent(landActivity, MainActivity::class.java)
        if (stateID != null) {
            accountService.openSession(stateID.accountId)
            intent.putExtra(AppNames.EXTRA_STATE, stateID.id)
        }
        landActivity.finish()
        startActivity(intent)
    }

    private suspend fun migrate(context: Context) {

        binding.currentStatus = "Preparing migration..."
        migrationService.prepare(context)
        delay(200)
        progress(20)
        binding.currentStatus = "Migrating accounts and credentials..."
        var currProg = 20
        var done = migrationService.migrateAccounts(50) {
            currProg += it.toInt()
            lifecycleScope.launch {
                progress(currProg)
            }
            true
        }

        if (done) {
            binding.currentStatus = "Cleaning legacy files..."
            // uploadFiles(context)
            migrationService.cleanLegacyFiles(context)
            prefs.setInt(
                AppNames.PREF_KEY_INSTALLED_VERSION_CODE,
                ClientData.getInstance().versionCode.toInt()
            )
            progress(95)
            delay(200)
        }

        binding.currentStatus = "You're good to go. Happy file sharing!"
        withContext(Dispatchers.Main) {
            chooseFirstPage()
        }
        progress(100)
        delay(3000)

        return
    }

    private fun needsMigration(context: Context): Boolean {
        val oldValue = prefs.getInt(AppNames.PREF_KEY_INSTALLED_VERSION_CODE)

        if (oldValue < 1 && !migrationService.hasLegacyDB(context)) {
            // New installation without legacy data
            prefs.setInt(
                AppNames.PREF_KEY_INSTALLED_VERSION_CODE,
                ClientData.getInstance().versionCode.toInt()
            )
            return false
        }

        if (oldValue < ClientData.getInstance().versionCode) {
            return if (migrationService.hasLegacyDB(context)) {
                true
            } else {
                Log.e(logTag, "Needs migration but cannot find legacy DB files, aborting")
                false
            }
        }
        return false
    }

    private suspend fun progress(newValue: Int) {
        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                binding.loadingIndicator.setProgress(newValue, true)
            } else {
                binding.loadingIndicator.setProgress(newValue)
            }
        }
    }

    // Helper to easily replay the migration: upload necessary files to one of the newly restored account
    // so that they are available. Put them in resource folder to relaunch the migration on an empty instance.
    private fun uploadFiles(context: Context) {
        val targetState = StateID("admin", "https://files.example.com", "/common-files")
        val mainDbFile = migrationService.dbFile(context, MainDB.DB_FILE_NAME)
        migrationService.doUpload(targetState, mainDbFile, SdkNames.NODE_MIME_DEFAULT)
        val syncDbFile = migrationService.dbFile(context, SyncDB.DB_FILE_NAME)
        migrationService.doUpload(targetState, syncDbFile, SdkNames.NODE_MIME_DEFAULT)
    }
}
