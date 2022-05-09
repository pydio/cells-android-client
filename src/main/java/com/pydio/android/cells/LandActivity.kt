package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.services.AccountService

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

            var stateID : StateID? = null
            // Get the app last recorded state
//            var stateID = CellsApp.instance.getCurrentState()
//
//            // We check if the last recorded state points toward a "forgotten" account
//            // TODO make this more reliable (Check if the full state still exists?)
//
//            if (stateID != null) {
//                val recordedState = stateID
//                stateID = withContext(Dispatchers.IO) {
//                    if (accountDao.getAccount(recordedState.accountId) == null) {
//                        Log.e(logTag, "no account found for $recordedState")
//                        return@withContext null
//                    } else {
//                        return@withContext recordedState
//                    }
//                }
//            }

//            if (stateID == null) { // No state or last state does not exist anymore

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
//                        CellsApp.instance.setCurrentState(stateID!!)
                    }
                    // else we navigate to the MainActivity with no state,
                    //  that should led us to the account list
                    //  size > 1 -> navController.navigate(MainNavDirections.openAccountList())
                }
//            }
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
