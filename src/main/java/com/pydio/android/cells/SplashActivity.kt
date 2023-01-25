package com.pydio.android.cells

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.pydio.android.cells.ui.box.LandingScreen
import com.pydio.android.cells.ui.theme.CellsTheme

/**
 * This is the main entry point of the app.
 * Instrumented with Jetpack Compose, it provides the splash screen
 * and forwards to the correct activity once the backend has been started.
 */
class SplashActivity : ComponentActivity() {

    private val logTag = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(logTag, "### onCreate()")
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CellsTheme {
                MainScreen(onInitialisationDone = {
                    Log.i(logTag, "### onInitialisationDone!!!!")
                })
            }
        }
    }
}

@Composable
private fun MainScreen(onInitialisationDone: () -> Unit) {
    Surface(color = MaterialTheme.colors.primary) {
        var showLandingScreen by remember { mutableStateOf(true) }
        if (showLandingScreen) {
            Log.i("MainScreen", "Show landing screen")
            LandingScreen(onTimeout = { showLandingScreen = false })
        } else {
            onInitialisationDone()
        }
    }
}

