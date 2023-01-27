package com.pydio.android.cells


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.box.AuthHost
import com.pydio.android.cells.ui.box.SelectTargetApp
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
            Log.e(logTag, "... Parsing intent for ${intent.action} - $extraUrl")
            when {
                Intent.ACTION_VIEW == intent.action -> {
                    val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
                    val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)
                    if (code != null && state != null) {
                        loginVM.setCurrentStep(LoginStep.OAUTH_FLOW)
                        lifecycleScope.launch {
                            loginVM.handleOAuthResponse(state, code)
                        }

                        // set processing
                        // go to
                        // FIXME
//                    val action = ServerUrlFragmentDirections.actionServerUrlToOauthFlow(null)
//                    findNavController(R.id.auth_fragment_host).navigate(action)
                    }
                }

                extraUrl != null -> {
                    // TODO double check that browsing is the relevant default here
                    val next: String = lazyGet(AppKeys.EXTRA_AFTER_AUTH_ACTION)
                        ?: AuthService.NEXT_ACTION_BROWSE
                    if (intent.getBooleanExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, false)) {
                        loginVM.toP8Credentials(extraUrl, next)
                    } else {
                        lifecycleScope.launch {
                            loginVM.toCellsCredentials(extraUrl, next)
                        }
                        // FIXME Forward to cells
//                        val action = ServerUrlFragmentDirections.actionServerUrlToOauthFlow(urlStr)
//                        findNavController(R.id.auth_fragment_host).navigate(action)
                    }
                }
                else -> {
                    Log.e(logTag, "... Unexpected intent: $intent")
                }
            }
        }

        val authActivity = this

        val afterAuth: (Boolean) -> Unit = {
            val accId = loginVM.accountID.value
            if (!it || accId == null) {
                // Auth has failed
                // TODO handle the case
                Log.e(logTag, "Could not launch after auth for $accId, flag: $it")
            } else {
                when (loginVM.nextAction) {
                    AuthService.NEXT_ACTION_ACCOUNTS,
                    AuthService.NEXT_ACTION_TERMINATE -> {
                    } // Do nothing => we return where we launched the auth process
                    AuthService.NEXT_ACTION_BROWSE -> {
                        // We have registered a new account and want to browse to it
                        val intent = Intent(authActivity, MainActivity::class.java)
                        intent.putExtra(AppKeys.EXTRA_STATE, accId)
                        Log.i(logTag, "Auth Successful, navigating to ${StateID.fromId(accId)}")
                        startActivity(intent)
                    }
                }
                authActivity.finish()
            }
        }

        setContent {
            SelectTargetApp {
                val navController = rememberNavController()
                AuthHost(
                    navController = navController,
                    loginVM = loginVM,
                    afterAuth = afterAuth
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
