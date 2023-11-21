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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.MainApp
import com.pydio.android.cells.ui.core.screens.WhiteScreen
import com.pydio.android.cells.ui.login.models.PreLaunchState
import com.pydio.android.cells.ui.login.models.PreLaunchVM
import com.pydio.android.cells.ui.login.screens.AuthScreen
import com.pydio.android.cells.ui.system.models.LandingVM
import com.pydio.cells.transport.StateID
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.KoinContext
import org.koin.core.parameter.parametersOf


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

        installSplashScreen()
        Log.i(logTag, "... onCreate for main activity, bundle: $savedInstanceState")
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        var appIsReady = false
        val mainActivity = this
        setContent {
            KoinContext {
                AppBinding(
                    activity = mainActivity,
                    sBundle = savedInstanceState,
                    intentID = intentIdentifier(),
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
    fun AppBinding(
        activity: Activity,
        sBundle: Bundle?,
        intentID: String,
        readyCallback: () -> Unit,
    ) {
        Log.e(logTag, "... Recomposing AppBinding: bundle:$sBundle, intent: $intentID")

        val landingVM by viewModel<LandingVM>()
        val preLaunchVM: PreLaunchVM = koinViewModel(parameters = { parametersOf(intentID) })

        val intentHasBeenProcessed = rememberSaveable { mutableStateOf(false) }

        val appState by preLaunchVM.appState.collectAsState()
        val processState by preLaunchVM.processState.collectAsState()

//         val startingState = remember { mutableStateOf<StartingState?>(null) }
        val ready = remember { mutableStateOf(false) }

//        val ackStartStateProcessed: (String?, StateID) -> Unit = { _, _ ->
//            intentHasBeenProcessed.value = true
//            startingState.value = null
//        }

        val emitActivityResult: (Int) -> Unit = { res ->
            setResult(res)
            when (res) {
                RESULT_CANCELED -> finishAndRemoveTask()
                RESULT_OK -> finish()
                else -> {} // Do nothing
            }
        }

        val processSelectedTarget: (StateID?) -> Unit = {
            Log.e(logTag, "... Process selected $it")
        }

        LaunchedEffect(key1 = intentID) {
            val msg = "... First composition for AppBinding with:" +
                    "\n\tintent: [$intentID]\n\tbundle: $sBundle "
            Log.e(logTag, msg)

            val noMigrationNeeded = landingVM.noMigrationNeeded()
            if (!noMigrationNeeded) { // forward to migration page
                Log.e(logTag, "## Forwarding to migration page and closing curr activity")
                val intent = Intent(activity, MigrateActivity::class.java)
                startActivity(intent)
                activity.finish()
                return@LaunchedEffect
            }

            if (intentHasBeenProcessed.value) {
                Log.w(logTag, "intent has already been processed...")
                preLaunchVM.skip()
            } else {
                handleIntent(preLaunchVM, sBundle)
            }
            readyCallback()
            intentHasBeenProcessed.value = true
        }

        // Rework this: we have the default for the time being.
        // see e.g https://medium.com/mobile-app-development-publication/android-jetpack-compose-inset-padding-made-easy-5f156a790979
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        val widthSizeClass = calculateWindowSizeClass(activity).widthSizeClass

        Box {

            when (processState) {
                // FIXME Handle the case where we have to explicitly navigate to a new page (e.G: new account...)
                PreLaunchState.DONE,
                PreLaunchState.SKIP ->
                    MainApp(
                        initialAppState = appState,
                        processSelectedTarget = processSelectedTarget,
                        emitActivityResult = emitActivityResult,
                        widthSizeClass = widthSizeClass,
                    )

                PreLaunchState.PROCESSING -> ProcessAuth(preLaunchVM)

                PreLaunchState.NEW -> {
                    Log.d(logTag, "... At Compose root, not yet ready")
                    WhiteScreen()
                }
            }
        }
    }

    @Composable
    fun ProcessAuth(loginVM: PreLaunchVM) {
        val message = loginVM.message.collectAsState()
        val errMsg = loginVM.errorMessage.collectAsState()
        AuthScreen(
            isProcessing = errMsg.value.isNullOrEmpty(),
            message = message.value,
            errMsg = errMsg.value,
            cancel = { TODO("Reimplement") }
        )
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

    private suspend fun handleIntent(preLaunchVM: PreLaunchVM, sBundle: Bundle?) {
        try {
            val msg = "... Processing intent ($intent):\n" +
                    "\t- cmp: ${intent.component}\n\t- action${intent.action}" +
                    "\n\t- categories: ${intent.categories}" //+
            Log.e(logTag, msg)
            if (sBundle != null) {
                TODO("Handle non-null saved bundle state")
            }

            // Handle various supported events
            when (intent.action) {
                Intent.ACTION_MAIN -> {
                    preLaunchVM.launchApp()
                }

                Intent.ACTION_VIEW -> {
                    val code = intent.data?.getQueryParameter(AppKeys.QUERY_KEY_CODE)
                    val state = intent.data?.getQueryParameter(AppKeys.QUERY_KEY_STATE)

                    if (code == null || state == null) {
                        throw IllegalArgumentException("Received an unexpected VIEW intent: $intent")
                    }

                    val (isValid, targetStateID) = preLaunchVM.isAuthStateValid(state)
                    if (!isValid) {
                        throw IllegalArgumentException("Passed state is wrong or already consumed: $intent")
                    }

                    preLaunchVM.handleOAuthCode(state, code)
                }

                Intent.ACTION_SEND -> {
                    val clipData = intent.clipData ?: run {
                        throw IllegalArgumentException("Cannot share with no clip data: $intent")
                    }
                    preLaunchVM.handleShare(clipData)
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    val clipData = intent.clipData ?: run {
                        throw IllegalArgumentException("Cannot share with no clip data: $intent")
                    }
                    preLaunchVM.handleShares(clipData)
                }

                else -> throw IllegalArgumentException("Unexpected intent: $intent")

            }
            // FIXME Handle errors here.
        } catch (e: Exception) {
            Log.e(logTag, "Could not handle intent, doing nothing...")
            e.printStackTrace()
//                if (e is SDKException) {
//                    Log.e(logTag, "After handleIntent, error thrown: ${e.code} - ${e.message}")
//                    if (e.code == ErrorCodes.unexpected_content) { // We should never have received this
//                        Log.e(logTag, "Launch activity with un-valid state, ignoring...")
//                        activity.finishAndRemoveTask()
//                        return@LaunchedEffect
//                    }
//                }
//                Log.e(logTag, "Could not handle intent, aborting....")
//                throw e
        }
    }

//    private suspend fun handleIntent(
//        landingVM: LandingVM
//    ): StartingState {
//        Log.d(logTag, "   => Processing intent: $intent")
//
//        // No intent => issue
//        if (intent == null) {
//            Log.e(logTag, "#############################")
//            Log.e(logTag, "No Intent and no bundle")
//            Thread.dumpStack()
//            Log.e(logTag, "#############################")
//            // TODO find how we can land here and fix.
//            val state = StartingState(StateID.NONE)
//            state.route = CellsDestinations.Accounts.route
//            return state
//        }
//
//        // Intent with a stateID => should not happen anymore
//        val encodedState = intent.getStringExtra(AppKeys.EXTRA_STATE)
//        val initialStateID = encodedState?.let {
//            val stateID = StateID.fromId(it)
//            // We must probably never pass here anymore
//            // TODO double check and clean this (and the corresponding AppKeys.EXTRA_STATE)
//            Log.e(logTag, "#### Received an intent with a state: $stateID")
//            stateID
//        } ?: StateID.NONE
//        var startingState = StartingState(initialStateID)
//
//        // Handle various supported events
//        when {
//
//            // Normal start
//            Intent.ACTION_MAIN == intent.action
//                    && intent.hasCategory(Intent.CATEGORY_LAUNCHER) -> {
//                startingState = landingVM.getStartingState()
//            }
//
//            Intent.ACTION_VIEW == intent.action -> {
//                val code = intent.data?.getQueryParameter(AppNames.QUERY_KEY_CODE)
//                val state = intent.data?.getQueryParameter(AppNames.QUERY_KEY_STATE)
//
//                if (code != null && state != null) { // Callback for OAuth credential flow
//                    val (isValid, targetStateID) = landingVM.isAuthStateValid(state)
//                    if (!isValid) {
//                        Log.e(
//                            logTag,
//                            "Received a OAuth flow callback intent, but it has already been consumed, ignoring "
//                        )
//                        throw SDKException(
//                            ErrorCodes.unexpected_content,
//                            "Passed state is wrong or already consumed"
//                        )
//                    }
//                    startingState.code = code
//                    startingState.state = state
//                    startingState.stateID = targetStateID
//                    startingState.route =
//                        LoginDestinations.ProcessAuthCallback.createRoute(targetStateID)
//
//                } else {
//                    Log.e(logTag, "Unexpected ACTION_VIEW: $intent")
//                    if (intent.extras != null) {
//                        Log.e(logTag, "Listing extras:")
//                        intent.extras?.keySet()?.let {
//                            for (key in it.iterator()) {
//                                Log.e(logTag, " - $key")
//                            }
//                        }
//                    }
//                }
//            }
//
//            Intent.ACTION_SEND == intent.action -> {
//                val clipData = intent.clipData
//                Log.d(logTag, "ACTION_SEND received, clipData: $clipData")
//                clipData?.let {
//                    startingState.route = ShareDestination.ChooseAccount.route
//                    clipData.getItemAt(0).uri?.let {
//                        startingState.uris.add(it)
//                    }
//                }
//            }
//
//            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
//                val tmpClipData = intent.clipData
//                tmpClipData?.let { clipData ->
//                    startingState.route = ShareDestination.ChooseAccount.route
//                    for (i in 0 until clipData.itemCount) {
//                        clipData.getItemAt(i).uri?.let {
//                            startingState.uris.add(it)
//                        }
//                    }
//                }
//            }
//
//            else -> {
//                val action = intent.action
//                var categories = ""
//                intent.categories?.forEach { categories += "$it, " }
//                Log.w(logTag, "... Unexpected intent: $action - $categories")
//            }
//        }
//        return startingState
//    }

    private fun intentIdentifier(): String {
        var id: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            id = intent.identifier
        }
        id = id ?: run {// tmp hack: compute a local ID based on a few variables
            "${intent.categories}/${intent.action}/${intent.component}"
        }
        return id
    }
}
