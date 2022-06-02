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
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.db.runtime.JobDao
import com.pydio.android.cells.db.runtime.RJob
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.CellsPreferences
import com.pydio.android.cells.services.FileService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.legacy.v2.MigrationServiceV2
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Launcher activity: it handles necessary migration and then decide which page
 * to open and transmit an intent to the MainActivity
 */
class LandActivity : AppCompatActivity() {

    private val logTag = LandActivity::class.simpleName

    private val prefs: CellsPreferences by inject()

    private val jobService by inject<JobService>()
    private val jobDao by inject<JobDao>()
    private val migrationService = MigrationServiceV2()

    private val accountService: AccountService by inject()
    private val accountDao: AccountDao by inject()
    private val sessionDao: SessionDao by inject()
    private val nodeService: NodeService by inject()

    private lateinit var binding: ActivityLandBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_land)

        if (intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true) {
            lifecycleScope.launch {
                if (needsMigration(applicationContext)) {
                    migrate()
                } else {
                    chooseFirstPage()
                }
            }
        }
        jobService.i(logTag, "### Application started","-")
    }

    private suspend fun migrate() {

        binding.migrationReportPanel.visibility = View.VISIBLE

        val migrationJob: RJob? = withContext(Dispatchers.IO) {
            val job = jobService.createAndLaunch(
                AppNames.JOB_OWNER_WORKER,
                AppNames.JOB_TEMPLATE_MIGRATION_V2,
                "Migration from v2 to v3",
                maxSteps = 100
            ) ?: return@withContext null
            jobService.i(logTag, "Created ${job.label}", "${job.jobId}")
            job
        }

        if (migrationJob == null) {
            jobService.e(logTag, "Could not create migration Job")
            return
        }
        jobDao.getLiveById(migrationJob.jobId).observe(this@LandActivity) {
            it?.let {
                val newValue = it.progress * 100 / it.total
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.loadingBar.setProgress(newValue.toInt(), true)
                } else {
                    binding.loadingBar.progress = newValue.toInt()
                }
                binding.currentStatus = it.progressMessage
                binding.executePendingBindings()
            }
        }
        val offlineRootsNb = withContext(Dispatchers.IO) {
            val nb = migrationService.migrate(applicationContext, migrationJob)
            jobService.i(
                logTag, "${migrationJob.label} terminated",
                "${migrationJob.jobId}"
            )
            nb
        }
        afterMigration(offlineRootsNb)
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
            else -> {
                // If a session is listed as in foreground, we open this one
                val currSession = withContext(Dispatchers.IO) { sessionDao.getForegroundSession() }
                currSession?.let { stateID = StateID.fromId(it.accountID) }
                    ?: let {
                        // TODO we should open the account list
                    }
            }
            // else we navigate to the MainActivity with no state,
            //  that should led us to the account list
            //  size > 1 -> navController.navigate(MainNavDirections.openAccountList())
        }
        val intent = Intent(landActivity, MainActivity::class.java)
        stateID?.let {
            accountService.openSession(it.accountId)
            intent.putExtra(AppNames.EXTRA_STATE, it.id)
        }
        landActivity.finish()
        startActivity(intent)
    }

    private suspend fun afterMigration(offlineRootsNb: Int) {

        val res = this@LandActivity.resources

        // change layout for next step
        binding.loadingBar.visibility = View.GONE
        binding.migrationTitle.text = res.getString(R.string.post_migration_title)
        // binding.currentStatus = res.getString(R.string.post_migration_message)

        binding.browseBtn.visibility = View.VISIBLE
        binding.browseBtn.setOnClickListener {
            lifecycleScope.launch {
                chooseFirstPage()
            }
        }

        if (offlineRootsNb == 0) {
            binding.browseBtn.text = res.getText(R.string.action_open_app)
        } else {
            binding.offlineNotMigratedDesc.visibility = View.VISIBLE
            binding.offlineNotMigratedDesc.text = resources.getString(R.string.post_migration_sync_message)
            binding.launchSyncBtn.text = resources.getText(R.string.button_resync_all)
            binding.launchSyncBtn.visibility = View.VISIBLE
            binding.launchSyncBtn.setOnClickListener {
                CellsApp.instance.appScope.launch {
                    withContext(Dispatchers.IO) {
                        nodeService.runFullSync("${AppNames.JOB_OWNER_USER} (post-migration)")
                    }
                }
                lifecycleScope.launch {
                    chooseFirstPage()
                }
            }

            binding.browseBtn.text = resources.getText(R.string.button_skip)
        }
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

//    private suspend fun progress(newValue: Int) {
//        withContext(Dispatchers.Main) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                binding.loadingIndicator.setProgress(newValue, true)
//            } else {
//                binding.loadingIndicator.progress = newValue
//            }
//        }
//    }

}
