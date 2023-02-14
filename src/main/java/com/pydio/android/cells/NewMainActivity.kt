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
import com.pydio.android.cells.ui.box.MainHost
import com.pydio.android.cells.ui.box.UseCellsTheme
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID

/**
 * Let the end-user choose a target in one of the defined remote servers.
 * This is both used for receiving intents from third-party applications and
 * for choosing a target location for copy and moved.
 */
class NewMainActivity : ComponentActivity() {

    private val logTag = NewMainActivity::class.simpleName

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(logTag, "onCreate: launching new main activity")
        super.onCreate(savedInstanceState)

        val encodedState = savedInstanceState?.getString(AppKeys.EXTRA_STATE)
            ?: intent.getStringExtra(AppKeys.EXTRA_STATE)

        val initialStateID = StateID.fromId(encodedState ?: Transport.UNDEFINED_STATE)
        Log.d(logTag, "onCreate for: $initialStateID")

        WindowCompat.setDecorFitsSystemWindows(window, false)


        val launchIntent: (Intent?, Boolean, Boolean) -> Unit =
            { intent, checkIfKnown, alsoFinishCurrentActivity ->
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

        setContent {

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass


            UseCellsTheme {
//
//                val navController = rememberNavController()
//                Scaffold {
//                    NavigationComponent(navController, it)
//                }
//            }

//                val launchTaskFor: (StateID, String?) -> Unit = { stateID, action ->
//                    when (val currAction: String = action ?: "none") {
//                        AppNames.ACTION_COPY, AppNames.ACTION_MOVE -> {
//                            finishAndRemoveTask()
//                        }
//                    }
//                }

                MainHost(
                    initialStateID,
                    launchIntent,
                    widthSizeClass,
                )
            }
        }
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
}
