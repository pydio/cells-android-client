package com.pydio.android.cells

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.MainApp
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.android.cells.ui.system.models.LandingVM
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main entry point for the Cells Application:
 *
 * - We check if we should forward to the migrate activity
 * - If no migration is necessary, we handle the bundle / intent and then forward everything to Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private val logTag = "MainActivity"

    private val connectionService: ConnectionService by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching new main activity")
        super.onCreate(savedInstanceState)

        // We currently still use androidx.core:core-splashscreen library to manage splash
        // Re-add the Splash composable here? - cf system/screens/splash.kt
        installSplashScreen()

        // First check if we need a migration
        val mainActivity = this
        lifecycleScope.launch {

            val landingVM by viewModel<LandingVM>()
            val noMigrationNeeded = landingVM.noMigrationNeeded()
            if (!noMigrationNeeded) { // forward to migration page
                val intent = Intent(mainActivity, MigrateActivity::class.java)
                startActivity(intent)
                mainActivity.finish()
                return@launch
            }

            val startingState = try {
                handleIntent(savedInstanceState, landingVM)
            } catch (e: SDKException) {
                Log.e(logTag, "After handleIntent, error thrown: ${e.code} - ${e.message}")
                if (e.code == ErrorCodes.unexpected_content) {
                    // We should never have received this
                    Log.e(logTag, "Received a launch activity with un-valid state, aborting....")
                    mainActivity.finishAndRemoveTask()
                    return@launch
                } else {
                    Log.e(logTag, "Could not handle intent, aborting....")
                    throw e
                }
            }

            if (Str.empty(startingState.route)) {
                // FIXME the state is not nul but we still don't know where to go.
                Log.e(logTag, "#### TODO state is not null but we still do not see where to go")
            }

            Log.i(logTag, "#######################################")
            Log.i(logTag, "onCreate with starting state:")
            Log.i(logTag, "  StateID: ${startingState.stateID}")
            Log.i(logTag, "  Route: ${startingState.route}")

            // Rework this: we have the default for the time being.
            // see e.g https://medium.com/mobile-app-development-publication/android-jetpack-compose-inset-padding-made-easy-5f156a790979
            // WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.setDecorFitsSystemWindows(window, true)

            setContent {
                val widthSizeClass = calculateWindowSizeClass(mainActivity).widthSizeClass
                val intentHasBeenProcessed = rememberSaveable {
                    mutableStateOf(false) // startingState == null)
                }

                val startingStateHasBeenProcessed: (String?, StateID) -> Unit = { _, _ ->
                    intentHasBeenProcessed.value = true
                }

                val launchTaskFor: (String, StateID) -> Unit = { action, stateID ->
                    when (action) {
                        AppNames.ACTION_CANCEL -> {
                            setResult(RESULT_CANCELED)
                            finishAndRemoveTask()
                        }

                        AppNames.ACTION_DONE -> {
//                            Log.e(logTag, "Action Done")
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }

                MainApp(
                    startingState = if (intentHasBeenProcessed.value) null else startingState,
                    startingStateHasBeenProcessed = startingStateHasBeenProcessed,
                    launchIntent = mainActivity::launchIntent,
                    launchTaskFor = launchTaskFor,
                    widthSizeClass = widthSizeClass,
                )
            }
            landingVM.recordLaunch()
        }
    }

    override fun onPause() {
        connectionService.pauseMonitoring()
        super.onPause()
    }

    override fun onResume() {
        connectionService.relaunchMonitoring()
        super.onResume()
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

    private suspend fun handleIntent(
        savedInstanceState: Bundle?,
        landingVM: LandingVM
    ): StartingState {
        Log.e(logTag, "#############################")
        Log.e(logTag, "Handle Intent: $intent")
        if (intent == null) { // we then rely on saved state or defaults
            if (savedInstanceState != null) {
                Log.e(logTag, "No intent **BUT WE HAVE A NON NULL BUNDLE**, investigate!")
                Log.e(logTag, "Saved state: " + savedInstanceState.describeContents())
                Thread.dumpStack()
            }
            // TO BE REFINED we assume we have no starting state in such case
            Log.e(logTag, "#############################")
            Log.e(logTag, "No Intent and no bundle")
            Thread.dumpStack()
            Log.e(logTag, "#############################")

            // TODO find how we can land here and fix.
            val state = StartingState(StateID.NONE)
            state.route = CellsDestinations.Accounts.route
            return state
        }

        // Intent is not null
        val encodedState = intent.getStringExtra(AppKeys.EXTRA_STATE)
        val initialStateID = encodedState?.let {
            val stateID = StateID.fromId(it)
            // We must probably never pass here anymore
            // TODO double check and clean this (and the corresponding AppKeys.EXTRA_STATE)
            Log.e(logTag, "#### Received an intent with a state: $stateID")
            stateID
        } ?: StateID.NONE
        var startingState = StartingState(initialStateID)

        when {
            Intent.ACTION_VIEW == intent.action -> {
                // Handle call back for OAuth credential flow
                val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
                val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)
                if (code != null && state != null) {
                    val (isValid, targetStateID) = landingVM.isAuthStateValid(state)
                    if (!isValid) {
                        throw SDKException(
                            ErrorCodes.unexpected_content,
                            "Passed state is wrong or already consumed"
                        )
                    }
                    startingState.route = LoginDestinations.ProcessAuth.route
                    startingState.code = code
                    startingState.state = state
                    startingState.stateID = targetStateID
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

            Intent.ACTION_SEND == intent.action -> {
                val clipData = intent.clipData
                Log.d(logTag, "ACTION_SEND received, clipData: $clipData")
                clipData?.let {
                    startingState.route = ShareDestination.ChooseAccount.route
                    clipData.getItemAt(0).uri?.let {
                        startingState.uris.add(it)
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                val tmpClipData = intent.clipData
                tmpClipData?.let { clipData ->
                    startingState.route = ShareDestination.ChooseAccount.route
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let {
                            startingState.uris.add(it)
                        }
                    }
                }
            }

            Intent.ACTION_MAIN == intent.action
                    && intent.hasCategory(Intent.CATEGORY_LAUNCHER) -> {
                startingState = landingVM.getStartingState()
            }

            else -> {
                val action = intent.action
                var categories = ""
                intent.categories?.forEach { categories += "$it, " }
                Log.w(logTag, "... Unexpected intent: $action - $categories")
            }
        }
        return startingState
    }
}
