package com.pydio.android.cells.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import com.pydio.cells.transport.StateID

// private const val logTag = "MainApp"

class AppState(
    stateID: StateID,
    val intentID: String?,
    val route: String?,
    val context: String?,
) {
    // We rather rely on the string version of the StateID so that AppState is serializable by default
    private val stateIDStr: String

    val stateID: StateID
        get() = StateID.fromId(stateIDStr)

    init {
        stateIDStr = stateID.id
    }

    companion object {
        val NONE = AppState(
            stateID = StateID.NONE,
            intentID = null,
            route = null,
            context = null
        )
    }
}

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
