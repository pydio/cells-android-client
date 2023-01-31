package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.box.AuthApp
import com.pydio.android.cells.ui.box.AuthHost
import com.pydio.android.cells.ui.models.LoginStep
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Authentication against Cells or Pydio8 */
class AuthActivity : ComponentActivity() {

    private val logTag = AuthActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching auth process")
        super.onCreate(savedInstanceState)

        val loginVM by viewModel<LoginVM>()

        if (intent != null) {
            val extraUrl = lazyGet(AppKeys.EXTRA_SERVER_URL)
            when {
                // Handle call back for OAuth credential flow
                Intent.ACTION_VIEW == intent.action -> {
                    val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
                    val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)
                    if (code != null && state != null) {
                        loginVM.setCurrentStep(LoginStep.PROCESS_AUTH)
                        lifecycleScope.launch {
                            loginVM.handleOAuthResponse(state, code)
                        }
                    }
                }
                // Re-log to an already registered server
                extraUrl != null -> {
                    // TODO double check that browsing is the relevant default here
                    val next = lazyGet(AppKeys.EXTRA_AFTER_AUTH_ACTION)
                        ?: AuthService.NEXT_ACTION_BROWSE
                    val isLegacy = intent.getBooleanExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, false)
                    Log.d(
                        logTag, "... Received a re-log cmd with $next flag, " +
                                "for ${if (isLegacy) "legacy P8" else "Cells"} server at $extraUrl"
                    )
                    if (isLegacy) {
                        loginVM.toP8Credentials(extraUrl, next)
                    } else {
                        lifecycleScope.launch {
                            loginVM.toCellsCredentials(extraUrl, next)
                        }
                    }
                }
                else -> {
                    Log.w(logTag, "... Unexpected intent: $intent")
                }
            }
        }

        val authActivity = this

        val afterAuth: (Boolean) -> Unit = {
            val next = loginVM.nextAction.value
            Log.i(logTag, "After auth, success: $it, next action: $next")

            // TODO this is not correctly retrieved if it is *not* a flow, it smells...
            val accId = loginVM.accountID.value
            if (!it) { // Activity canceled by end-user
                authActivity.finishAndRemoveTask()
            } else if (accId == null) {
                // Auth has failed // TODO handle the case
                val msg = "Can't launch action after (successful?) auth, no accountID has been set"
                Log.e(logTag, msg)
            } else {
                when (next) {
                    AuthService.NEXT_ACTION_ACCOUNTS,
                    AuthService.NEXT_ACTION_TERMINATE -> { // We return where we launched the auth process
                        authActivity.finishAndRemoveTask()
                    }
                    AuthService.NEXT_ACTION_SHARE -> { // We return to the share task
                        // TODO this does not work yet:
                        //  when we launch the process, we get out of the first share task
                        //  and did not achieve yet to call it back: we are always in a
                        //  new share activity within the current task.
                        val nextIntent = Intent(authActivity, SelectTargetActivity::class.java)
                        nextIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        Log.i(logTag, "Auth Successful, back to share with $nextIntent")
                        startActivity(nextIntent)
                        authActivity.finish()
                    }
                    AuthService.NEXT_ACTION_BROWSE -> { // We have registered a new account and want to browse to it
                        val intent = Intent(authActivity, MainActivity::class.java)
                        intent.putExtra(AppKeys.EXTRA_STATE, accId)
                        Log.i(logTag, "Auth Successful, navigating to ${StateID.fromId(accId)}")
                        startActivity(intent)
                        authActivity.finish()
                    }
                }
            }
        }

        val launchOAuth: (Intent) -> Unit = {
            Log.d(logTag, "Launching OAuth flow with $it")
            authActivity.startActivity(it)
            authActivity.finishAndRemoveTask()
        }

        setContent {
            AuthApp {
                val navController = rememberNavController()
                AuthHost(
                    navController = navController,
                    loginVM = loginVM,
                    afterAuth = afterAuth,
                    launchOAuth = launchOAuth,
                )
            }
        }
    }

    private fun lazyGet(key: String): String? {
        if (intent.hasExtra(key) && Str.notEmpty(intent.getStringExtra(key))) {
            return intent.getStringExtra(key)
        }
        return null
    }
}
