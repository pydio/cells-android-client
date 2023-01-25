package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.db.accounts.SessionDao
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.JobService
import com.pydio.android.cells.ui.model.MigrationVM
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent

/**
 *  The Land activity exposes the splash screen and in background:
 *  - first: checks if a migration is necessary and forward to the migration activity in such case.
 *  - Second: compute first stateID and forward to main activity.
 */
class LandActivity : AppCompatActivity() {

    private val logTag = LandActivity::class.simpleName

    private val jobService: JobService by KoinJavaComponent.inject(JobService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(logTag, "onCreate()")
        super.onCreate(savedInstanceState)

        // Use androidx.core:core-splashscreen library to manage splash
        installSplashScreen()
        setContentView(R.layout.activity_splash)

        val landActivity = this

        lifecycleScope.launch {
            val migrationVM by viewModel<MigrationVM>()
            val needsMigration = migrationVM.needsMigration(applicationContext)
            if (needsMigration) {
                // forward to migration page
                val intent = Intent(landActivity, MigrateActivity::class.java)
                startActivity(intent)
                landActivity.finish()
                return@launch
            }

            val accountService: AccountService by KoinJavaComponent.inject(AccountService::class.java)
            val accountDao: AccountDao by KoinJavaComponent.inject(AccountDao::class.java)
            val sessionDao: SessionDao by KoinJavaComponent.inject(SessionDao::class.java)

            val intent = Intent(landActivity, MainActivity::class.java)

            val landVM = LandViewModel(accountDao, sessionDao, accountService)
            val laterState = landVM.getStartStateID()
            laterState?.let {
                intent.putExtra(AppKeys.EXTRA_STATE, it.id)
            }

            startActivity(intent)
            recordLaunch()
            landActivity.finish()
        }
    }

    private fun recordLaunch() {
        try {
            val creationMsg = "### Starting agent ${ClientData.getInstance().userAgent()}"
            jobService.i(logTag, creationMsg, "Cells App")
//            jobService.d(logTag, ".... Testing log levels:", "DEBUGGER")
//            jobService.i(logTag, "   check - 1", "DEBUGGER")
//            jobService.w(logTag, "   check - 2. with a very very very very very very, very very very very looooong message!!!!!!!", "DEBUGGER")
//            jobService.e(logTag, "   check - 3", "DEBUGGER")
        } catch (e: Exception) {
            Log.e(logTag, "could not log start: $e")
        }
    }
}

//class LandViewModel(
//    val accountDao: AccountDao,
//    val sessionDao: SessionDao,
//    val accountService: AccountService,
//) : ViewModel() {
private class LandViewModel(
    val accountDao: AccountDao,
    val sessionDao: SessionDao,
    val accountService: AccountService,
) {

    suspend fun getStartStateID(): StateID? {
        var stateID: StateID? = null
        // Fallback on defined accounts:
        val accounts = withContext(Dispatchers.IO) { accountDao.getAccounts() }
        when (accounts.size) {
            0 -> stateID = null
            1 -> stateID = StateID.fromId(accounts[0].accountID)
            else -> {
                // If a session is listed as in foreground, we open this one
                val currSession = withContext(Dispatchers.IO) { sessionDao.getForegroundSession() }
                currSession?.let { stateID = StateID.fromId(it.accountID) }
            }
        }

        // probably useless, TODO double check
        stateID?.let {
            accountService.openSession(it.accountId)
        }
        return stateID
    }
}
