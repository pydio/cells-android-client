package com.pydio.android.cells.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.pydio.android.cells.ui.models.AccountListVM
import com.pydio.android.cells.ui.models.BrowseRemoteVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.stateIDSaver
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import org.koin.androidx.compose.koinViewModel

private const val logTag = "MainApp.kt"

@Composable
fun MainApp(
    startingState: StartingState?,
    startingStateHasBeenProcessed: (String?, StateID) -> Unit,
    launchIntent: (Intent?, Boolean, Boolean) -> Unit,
    widthSizeClass: WindowWidthSizeClass,
//    browseRemoteVM: BrowseRemoteVM = koinViewModel(),
//    accountListVM: AccountListVM = koinViewModel(),
) {

//    val currAccountID = rememberSaveable(stateSaver = stateIDSaver) {
//        mutableStateOf(startingState.stateID)
//    }
//
//    val openAccount: (StateID) -> Unit = {
//        currAccountID.value = it
//        Log.e(logTag, "--- Open Account: $it")
//    }

    NavHostWithDrawer(
        startingState = startingState,
        startingStateHasBeenProcessed = startingStateHasBeenProcessed,
//        currAccountID = currAccountID.value,
//        openAccount = openAccount,
        launchIntent = launchIntent,
        widthSizeClass = widthSizeClass
    )
}

@Composable
fun UseCellsTheme(content: @Composable () -> Unit) {
    CellsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

class StartingState(val stateID: StateID) {

    enum class Key {
        STATE_ID, DESTINATION, CODE, STATE, URIS
    }

    var destination: String? = null

    // OAuth credential flow call back
    var code: String? = null
    var state: String? = null

    // Share with Pydio
    var uris: MutableList<Uri> = mutableListOf()
}

// Converts a StartingState object which we don't know how to save to a Map<String, String> which we can save
val StartingStateSaver = Saver<StartingState, Map<String, String>>(

    save = { startingState ->
        val currMap = mutableMapOf<String, String>()
        currMap[StartingState.Key.STATE_ID.name] = startingState.stateID.id
        startingState.destination?.let {
            currMap[StartingState.Key.DESTINATION.name] = it
        }
        startingState.code?.let {
            currMap[StartingState.Key.CODE.name] = it
        }
        startingState.state?.let {
            currMap[StartingState.Key.STATE.name] = it
        }
        var uriStr = ""
        startingState.uris.forEach { uriStr = "$uriStr;$it" }

        if (Str.notEmpty(uriStr)) {
            currMap[StartingState.Key.URIS.name] = uriStr.substring(1)
        }
        currMap
    },

    restore = { values ->
        val stateID = StateID.fromId(values[StartingState.Key.STATE_ID.name])
        val startingState = StartingState(stateID)
        if (values.containsKey(StartingState.Key.DESTINATION.name)) {
            startingState.destination = values[StartingState.Key.DESTINATION.name]
        }
        if (values.containsKey(StartingState.Key.CODE.name)) {
            startingState.code = values[StartingState.Key.CODE.name]
        }
        if (values.containsKey(StartingState.Key.STATE.name)) {
            startingState.state = values[StartingState.Key.STATE.name]
        }
        if (values.containsKey(StartingState.Key.URIS.name)) {
            values[StartingState.Key.URIS.name]?.split(";")?.forEach {
                startingState.uris.add(Uri.parse(it))
            }
        }
        startingState
    }
)
