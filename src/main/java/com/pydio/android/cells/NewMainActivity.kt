package com.pydio.android.cells

import android.app.Activity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.view.WindowCompat
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.MainApp
import com.pydio.android.cells.ui.StartingState
import com.pydio.android.cells.ui.UseCellsTheme
import com.pydio.android.cells.ui.login.RouteLoginProcessAuth
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        val startingState = handleIntent(savedInstanceState)
        Log.d(logTag, "onCreate for: ${startingState?.stateID}")

        // TODO rework this
        // WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val intentHasBeenProcessed = rememberSaveable() {
                mutableStateOf(startingState == null)
            }

            val startingStateHasBeenProcessed: (String?, StateID) -> Unit = { _, _ ->
                intentHasBeenProcessed.value = true
            }

            val launchTaskFor: (String, StateID) -> Unit = { action, stateID ->
                when (action) {
                    AppNames.ACTION_CANCEL -> {
                        finishAndRemoveTask()
                    }

//                    AppNames.ACTION_LOGIN -> {
//                        // TODO this does not work when remote is Cells and we have to perform the OAuth flow.
//                        //    we haven't found yet a way to call back this task once the process has succeed.
//                        coroutineScope.launch {
//                            val session = withContext(Dispatchers.IO) {
//                                accountListVM.getSession(stateID)
//                            } ?: return@launch
//
//                            // TODO clean this when implementing custom certificate acceptance.
//                            val serverURL =
//                                ServerURLImpl.fromAddress(session.url, session.tlsMode == 1)
//                            val toAuthIntent = Intent(ctx, LoginActivity::class.java)
//                            toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_URL, serverURL.toJson())
//                            toAuthIntent.putExtra(
//                                AppKeys.EXTRA_SERVER_IS_LEGACY,
//                                session.isLegacy
//                            )
//                            toAuthIntent.putExtra(
//                                AppKeys.EXTRA_AFTER_AUTH_ACTION,
//                                AuthService.NEXT_ACTION_SHARE
//                            )
//                            // We don't want that the login intermediary activity pollute the history of the end user
//                            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//                            startActivity(toAuthIntent)
//                        }
//                    }
//                    AppNames.ACTION_UPLOAD -> {
//                        uploadsVM.launchShareToPydioAt(stateID, uris)
////                            finishAndRemoveTask()
//                    }
//                    AppNames.ACTION_CREATE_FOLDER -> {
//                        createFolderParent.value = stateID.id
//                        showCreateFolderDialog.value = true
////                            createFolder(ctx, stateID, nodeService)
//                    }
                }
            }

            UseCellsTheme {
//                val currState = rememberSaveable(stateSaver = StartingStateSaver) {
//                    mutableStateOf(startingState)
//                }
                MainApp(
                    // currState.value,
                    if (intentHasBeenProcessed.value) null else startingState,
                    startingStateHasBeenProcessed,
                    this::launchIntent,
                    launchTaskFor =launchTaskFor,
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

    private fun handleIntent(savedInstanceState: Bundle?): StartingState? {
        Log.e(logTag, "########################################")
        Log.e(logTag, "Handle Intent: $intent")
        if (intent == null) { // we then rely on saved state or defaults

            // TO BE REFINED we assume we have no starting state in such case
            return null

//            Log.e(logTag, "No Intent, we rely on the saved bundle: $savedInstanceState")
//            val encodedState = savedInstanceState?.getString(AppKeys.EXTRA_STATE)
//            val initialStateID = StateID.fromId(encodedState ?: Transport.UNDEFINED_STATE)
//            return StartingState(initialStateID)

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
