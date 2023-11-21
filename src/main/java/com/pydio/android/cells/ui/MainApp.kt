package com.pydio.android.cells.ui

import android.net.Uri
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import com.pydio.cells.transport.StateID

// private const val logTag = "MainApp"

class AppState(
    val stateID: StateID,
    val intentID: String?,
    val route: String?,
    val context: String?,
) {
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

class StartingState(var stateID: StateID) {
    var route: String? = null

    // OAuth credential flow call back
    var code: String? = null
    var state: String? = null

    // Share with Pydio
    var uris: MutableList<Uri> = mutableListOf()
}

//// Converts a StartingState object which we don't know how to save to a Map<String, String> which we can save
//val StartingStateSaver = Saver<StartingState, Map<String, String>>(
//
//    save = { startingState ->
//        val currMap = mutableMapOf<String, String>()
//        currMap[StartingState.Key.STATE_ID.name] = startingState.stateID.id
//        startingState.route?.let {
//            currMap[StartingState.Key.ROUTE.name] = it
//        }
//        startingState.code?.let {
//            currMap[StartingState.Key.CODE.name] = it
//        }
//        startingState.state?.let {
//            currMap[StartingState.Key.STATE.name] = it
//        }
//        var uriStr = ""
//        startingState.uris.forEach { uriStr = "$uriStr;$it" }
//
//        if (Str.notEmpty(uriStr)) {
//            currMap[StartingState.Key.URIS.name] = uriStr.substring(1)
//        }
//        currMap
//    },
//
//    restore = { values ->
//        val stateID = StateID.fromId(values[StartingState.Key.STATE_ID.name])
//        val startingState = StartingState(stateID)
//        if (values.containsKey(StartingState.Key.ROUTE.name)) {
//            startingState.route = values[StartingState.Key.ROUTE.name]
//        }
//        if (values.containsKey(StartingState.Key.CODE.name)) {
//            startingState.code = values[StartingState.Key.CODE.name]
//        }
//        if (values.containsKey(StartingState.Key.STATE.name)) {
//            startingState.state = values[StartingState.Key.STATE.name]
//        }
//        if (values.containsKey(StartingState.Key.URIS.name)) {
//            values[StartingState.Key.URIS.name]?.split(";")?.forEach {
//                startingState.uris.add(Uri.parse(it))
//            }
//        }
//        startingState
//    }
//)
