package com.pydio.android.cells.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import com.pydio.android.cells.ui.models.AppState
import com.pydio.cells.transport.StateID

@Composable
fun MainApp(
    initialAppState: AppState,
    processSelectedTarget: (StateID?) -> Unit,
    emitActivityResult: (Int) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {
    NavHostWithDrawer(
        initialAppState = initialAppState,
        processSelectedTarget = processSelectedTarget,
        emitActivityResult = emitActivityResult,
        widthSizeClass = widthSizeClass,
    )
}
