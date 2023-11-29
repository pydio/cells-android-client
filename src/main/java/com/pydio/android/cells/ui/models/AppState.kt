package com.pydio.android.cells.ui.models

import com.pydio.cells.transport.StateID

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