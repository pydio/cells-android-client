package com.pydio.android.cells.ui.system.models

import android.content.Context
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showLongMessage
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logTag = SettingsVM::class.simpleName

/** Expose methods used to perform house keeping on the App */
class SettingsVM(
    private val nodeService: NodeService
) : ViewModel() {

    // TODO

}
