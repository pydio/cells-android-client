package com.pydio.android.cells

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.view.WindowCompat
import com.pydio.android.cells.ui.MainApp
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.UseCellsTheme
import com.pydio.android.cells.ui.login.RouteLoginProcessAuth
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

/**
 * Main entry point for the Cells Application: we first handle the bundle / intent and then
 * forward everything to Jetpack Compose.
 */
class NewMainActivity : ComponentActivity() {

    private val logTag = NewMainActivity::class.simpleName

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching new main activity")
        super.onCreate(savedInstanceState)

        val startingState = handleInput(savedInstanceState)
        Log.d(logTag, "onCreate for: ${startingState.stateID}")

        // TODO rework this
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            UseCellsTheme {
//                val currState = rememberSaveable(stateSaver = StartingStateSaver) {
//                    mutableStateOf(startingState)
//                }
                MainApp(
                    // currState.value,
                    startingState,
                    this::launchIntent,
                    widthSizeClass,
                )
            }
        }
    }

    private fun launchIntent(
        intent: Intent?,
        checkIfKnown: Boolean,
        alsoFinishCurrentActivity: Boolean
    ) {
        if (intent == null) {
            finishAndRemoveTask()
        } else if (checkIfKnown) {
            val resolvedActivity =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val flag = PackageManager.ResolveInfoFlags
                        .of(MATCH_DEFAULT_ONLY.toLong())
                    packageManager.resolveActivity(intent, flag)
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY)
                }
            // TODO better error handling
            if (resolvedActivity == null) {
                Log.e(logTag, "No Matching handler found for $intent")
            }
        } else {
            startActivity(intent)
            if (alsoFinishCurrentActivity) {
                finishAndRemoveTask()
            }
        }
    }

    private fun handleInput(savedInstanceState: Bundle?): StartingState {

        if (intent == null) { // we then rely on saved state or defaults
            Log.e(logTag, "No Intent, we rely on the saved bundle: $savedInstanceState")
            val encodedState = savedInstanceState?.getString(AppKeys.EXTRA_STATE)
            val initialStateID = StateID.fromId(encodedState ?: Transport.UNDEFINED_STATE)
            return StartingState(initialStateID)
        }

        // Intent is not null
        val encodedState = intent.getStringExtra(AppKeys.EXTRA_STATE)
        val initialStateID = StateID.fromId(encodedState ?: Transport.UNDEFINED_STATE)
        var startingState = StartingState(initialStateID)

        val extraUrl = lazyGet(intent, AppKeys.EXTRA_SERVER_URL)
        when {
            Intent.ACTION_VIEW == intent.action -> {

                // Handle call back for OAuth credential flow
                val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
                val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)
                if (code != null && state != null) {
                    startingState.destination = RouteLoginProcessAuth.route
                    startingState.code = code
                    startingState.state = state
                } else {
                    Log.e(logTag, "Unexpected ACTION_VIEW: $intent")
                    if (intent.extras != null) {
                        Log.e(logTag, "Listing extras:")
                        intent.extras?.keySet()?.let {
                            for (key in it.iterator()) {
                                Log.e(logTag, " - $key")
                            }
                        }
                    }
                }
            }
            // Re-log to an already registered server
// TODO
//            extraUrl != null -> {
//                // TODO double check that browsing is the relevant default here
//                val next = lazyGet(AppKeys.EXTRA_AFTER_AUTH_ACTION)
//                    ?: AuthService.NEXT_ACTION_BROWSE
//                val isLegacy = intent.getBooleanExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, false)
//                Log.d(
//                    logTag, "... Received a re-log cmd with $next flag, " +
//                            "for ${if (isLegacy) "legacy P8" else "Cells"} server at $extraUrl"
//                )
//                if (isLegacy) {
//                    loginVM.toP8Credentials(extraUrl, next)
//                } else {
//                    lifecycleScope.launch {
//                        loginVM.toCellsCredentials(extraUrl, next)
//                    }
//                }
//            }
            else -> {
                Log.w(logTag, "... Unexpected intent: $intent")
            }
        }
        return startingState
    }


    override fun onDestroy() {
        Log.d(logTag, "onDestroy")
        super.onDestroy()
    }

    override fun onPause() {
        Log.d(logTag, "onPause")
        super.onPause()
    }

    override fun onResume() {
        Log.d(logTag, "onResume")
        super.onResume()
    }

    // Helpers
    private fun lazyGet(intent: Intent, key: String): String? {
        if (intent.hasExtra(key) && Str.notEmpty(intent.getStringExtra(key))) {
            return intent.getStringExtra(key)
        }
        return null
    }
}
