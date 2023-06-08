package com.pydio.android.cells.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.ErrorService
import com.pydio.cells.transport.StateID
import org.koin.core.component.KoinComponent

class BrowseRemoteVM(
    private val connectionService: ConnectionService,
    private val errorService: ErrorService
) : ViewModel(), KoinComponent {

    private val logTag = "BrowseRemoteVM"

    val loadingState = connectionService.loadingState
    val isLegacy = connectionService.isRemoteLegacy

    fun watch(newStateID: StateID, isForceRefresh: Boolean) {
        connectionService.setCurrentStateID(newStateID)
        if (isForceRefresh) {
            connectionService.forceRefresh()
        }
    }

    fun pause(oldID: StateID) {
        connectionService.pause(oldID)
    }

    init {
        Log.e(logTag, "#################################################")
        Log.e(logTag, "... Main browse view model has been initialised")
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(logTag, "cleared")
    }
}
