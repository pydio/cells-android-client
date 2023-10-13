package com.pydio.android.cells.ui.system.models

import android.content.Context
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.CoroutineService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Expose methods used to perform house keeping on the App */
class HouseKeepingVM(
    private val coroutineService: CoroutineService,
    private val nodeService: NodeService
) : ViewModel() {

    // private val logTag = "HouseKeepingVM"

    fun clearCache(context: Context, stateID: StateID) {
        coroutineService.cellsIoScope.launch {
            nodeService.clearAccountCache(stateID)
                ?.let {
                    withContext(Dispatchers.Main) {
                        showLongMessage(context, it) // TODO fix this
                    }
                }
        }
    }
}
