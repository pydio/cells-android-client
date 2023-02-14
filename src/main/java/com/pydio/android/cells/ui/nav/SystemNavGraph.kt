package com.pydio.android.cells.ui.nav

import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pydio.android.cells.ui.box.system.AboutScreen
import com.pydio.cells.utils.Log

/**
 * Main destinations used in Cells App
 */
private val logTag = "SystemNavGraph"

fun NavGraphBuilder.systemNavGraph(
    isExpandedScreen: Boolean,
    openDrawer: () -> Unit = {},
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    back: () -> Unit,
) {

    composable(SystemDestinations.ABOUT_ROUTE) {
        Log.e(logTag, "... Will navigate to About, expanded screen: $isExpandedScreen")
        AboutScreen(
            openDrawer = openDrawer,
            launchIntent = launchIntent,
            contentPadding = rememberContentPaddingForScreen(
                additionalTop = if (!isExpandedScreen) 0.dp else 8.dp,
                excludeTop = !isExpandedScreen
            ),
        )
    }
}
