package com.pydio.android.cells

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.MainApp
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.core.nav.CellsDestinations
import com.pydio.android.cells.ui.core.screens.WhiteScreen
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.android.cells.ui.system.models.LandingVM
import com.pydio.cells.api.ErrorCodes
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.StateID
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.KoinContext


/**
 * Main entry point for the Cells Application:
 *
 * - We check if we should forward to the migrate activity
 * - If no migration is necessary, we handle the bundle / intent and then forward everything to Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private val logTag = "MainActivity"

    private val connectionService: ConnectionService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(logTag, "... onCreate for main activity, bundle: $savedInstanceState")

        // We use androidx.core:core-splashscreen library to manage splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        val mainActivity = this
        WindowCompat.setDecorFitsSystemWindows(window, true)

        var appIsReady = false
        setContent {
            KoinContext {
                MainActivityContent(
                    activity = mainActivity,
                    sBundle = savedInstanceState,
                    launchIntent = mainActivity::launchIntent
                ) {
                    appIsReady = true
                }
            }
        }

        // Set up an OnPreDrawListener to the root view.
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Check whether the initial data is ready.
                    return if (appIsReady) {
                        // The content is ready. Start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // The content isn't ready. Suspend.
                        false
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun MainActivityContent(
        activity: Activity,
        sBundle: Bundle?,
        launchIntent: (Intent?, Boolean, Boolean) -> Unit,
        readyCallback: () -> Unit,
    ) {
        Log.e(logTag, "... Recomposing MainActivityContent with i: $intent and b: $sBundle ")
        val landingVM by viewModel<LandingVM>()

        val widthSizeClass = calculateWindowSizeClass(activity).widthSizeClass
        val intentHasBeenProcessed = rememberSaveable { mutableStateOf(false) }
        val startingState = remember { mutableStateOf<StartingState?>(null) }
        val ready = remember { mutableStateOf(false) }

        val ackStartStateProcessed: (String?, StateID) -> Unit = { _, _ ->
            intentHasBeenProcessed.value = true
            startingState.value = null
        }

        val launchTaskFor: (String, StateID) -> Unit = { action, _ ->
            when (action) {
                AppNames.ACTION_CANCEL -> {
                    setResult(RESULT_CANCELED)
                    finishAndRemoveTask()
                }

                AppNames.ACTION_DONE -> {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }

        LaunchedEffect(key1 = intent.toString()) {
            Log.e(logTag, "## Launching main effect for $intent")
            Log.e(logTag, "\t\tIntent already processed: ${intentHasBeenProcessed.value}")

            // First quick check to detect necessary migration. Returns "true" in case of doubt to trigger further checks.
            val noMigrationNeeded = landingVM.noMigrationNeeded()
            if (!noMigrationNeeded) { // forward to migration page
                Log.e(logTag, "## Forwarding to migration page and closing curr activity")
                val intent = Intent(activity, MigrateActivity::class.java)
                startActivity(intent)
                activity.finish()
                return@LaunchedEffect
            }

            try { // We only handle intent when we have no bundle state
                sBundle ?: run {
                    startingState.value = handleIntent(landingVM)
                }
            } catch (e: SDKException) {
                Log.e(logTag, "After handleIntent, error thrown: ${e.code} - ${e.message}")
                if (e.code == ErrorCodes.unexpected_content) { // We should never have received this
                    Log.e(logTag, "Launch activity with un-valid state, ignoring...")
                    activity.finishAndRemoveTask()
                    return@LaunchedEffect
                } else {
                    Log.e(logTag, "Could not handle intent, aborting....")
                    throw e
                }
            }

            Log.i(logTag, "############################")
            Log.d(logTag, "  onCreate with starting state:")
            Log.d(logTag, "   StateID: ${startingState.value?.stateID}")
            Log.d(logTag, "   Route: ${startingState.value?.route}")

            // Rework this: we have the default for the time being.
            // see e.g https://medium.com/mobile-app-development-publication/android-jetpack-compose-inset-padding-made-easy-5f156a790979
            // WindowCompat.setDecorFitsSystemWindows(window, false)
            // WindowCompat.setDecorFitsSystemWindows(window, true)
            ready.value = true
            readyCallback()
        }

        Box {
            if (ready.value) {
                Log.d(logTag, "... Now ready, composing for ${startingState.value?.route}")
                MainApp(
                    startingState = startingState.value,
                    ackStartStateProcessed = ackStartStateProcessed,
                    launchIntent = launchIntent,
                    launchTaskFor = launchTaskFor,
                    widthSizeClass = widthSizeClass,
                )
            } else {
                WhiteScreen()
            }
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
        landingVM: LandingVM
    ): StartingState {
        Log.d(logTag, "   => Processing intent: $intent")

        // No intent => issue
        if (intent == null) {
            Log.e(logTag, "#############################")
            Log.e(logTag, "No Intent and no bundle")
            Thread.dumpStack()
            Log.e(logTag, "#############################")
            // TODO find how we can land here and fix.
            val state = StartingState(StateID.NONE)
            state.route = CellsDestinations.Accounts.route
            return state
        }

        // Intent with a stateID => should not happen anymore
        val encodedState = intent.getStringExtra(AppKeys.EXTRA_STATE)
        val initialStateID = encodedState?.let {
            val stateID = StateID.fromId(it)
            // We must probably never pass here anymore
            // TODO double check and clean this (and the corresponding AppKeys.EXTRA_STATE)
            Log.e(logTag, "#### Received an intent with a state: $stateID")
            stateID
        } ?: StateID.NONE
        var startingState = StartingState(initialStateID)

        // Handle various supported events
        when {

            // Normal start
            Intent.ACTION_MAIN == intent.action
                    && intent.hasCategory(Intent.CATEGORY_LAUNCHER) -> {
                startingState = landingVM.getStartingState()
            }

            Intent.ACTION_VIEW == intent.action -> {
                val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
                val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)

                if (code != null && state != null) { // Callback for OAuth credential flow
                    val (isValid, targetStateID) = landingVM.isAuthStateValid(state)
                    if (!isValid) {
                        Log.e(
                            logTag,
                            "Received a OAuth flow callback intent, but it has already been consumed, ignoring "
                        )
                        throw SDKException(
                            ErrorCodes.unexpected_content,
                            "Passed state is wrong or already consumed"
                        )
                    }
                    startingState.code = code
                    startingState.state = state
                    startingState.stateID = targetStateID
                    startingState.route =
                        LoginDestinations.ProcessAuthCallback.createRoute(targetStateID)

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
