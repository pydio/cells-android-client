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
class HouseKeepingVM(
    private val nodeService: NodeService
) : ViewModel() {

    fun clearCache(context: Context, stateID: StateID) {
        CellsApp.instance.appScope.launch {
            withContext(Dispatchers.IO) {
                nodeService.clearAccountCache(stateID)
                    ?.let {
                        withContext(Dispatchers.Main) {
                            showLongMessage(context, it) // TODO fix this
                        }
                    }
            }
        }
    }
}


//
//fun clearCache(
//    context: Context,
//    encodedState: String,
//    nodeService: NodeService,
//): Boolean {
//
//    MaterialAlertDialogBuilder(context)
//        .setTitle(R.string.confirm_cache_deletion_title)
//        .setIcon(R.drawable.ic_baseline_delete_24)
//        .setMessage(context.resources.getString(R.string.confirm_cache_deletion_message))
//        .setPositiveButton(R.string.button_confirm) { _, _ ->
//            doClearCache(context, encodedState, nodeService)
//        }
//        .setNegativeButton(R.string.button_cancel, null)
//        .show()
//    return true
//}
//
//private fun doClearCache(context: Context, encodedState: String, nodeService: NodeService) {
//    CellsApp.instance.appScope.launch {
//        withContext(Dispatchers.IO) {
//            nodeService.clearAccountCache(encodedState)
//                ?.let {
//                    withContext(Dispatchers.Main) {
//                        showLongMessage(context, it)
//                    }
//                }
//        }
//    }
//}
