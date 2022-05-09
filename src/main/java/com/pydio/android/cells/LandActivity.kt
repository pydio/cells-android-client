package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.services.AccountService
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class LandActivity : AppCompatActivity() {

    private val logTag = LandActivity::class.simpleName
    private val accountService: AccountService by inject()
    private val accountDao: AccountDao by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate")

        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_land)

        if (intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) == true) {
            chooseFirstPage()
        }
    }

    private fun chooseFirstPage() {
        val landActivity = this
        lifecycleScope.launch {

            var stateID: StateID? = null
            // Fallback on defined accounts:
            val accounts = withContext(Dispatchers.IO) { accountDao.getAccounts() }
            when (accounts.size) {
                0 -> { // No account: launch registration
                    startActivity(Intent(landActivity, AuthActivity::class.java))
                    landActivity.finish()
                    return@launch
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
    }
}
