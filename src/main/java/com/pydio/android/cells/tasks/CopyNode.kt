package com.pydio.android.cells.tasks

import android.content.Context
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.utils.showMessage

fun copyNodes(
    context: Context,
    sources: List<StateID>,
    target: StateID,
    nodeService: NodeService,
): Boolean {

    if (sources.isEmpty()) {
        return false
    }
    doCopyNodes(context, sources, target, nodeService)
    return true
}

private fun doCopyNodes(
    context: Context,
    sources: List<StateID>,
    targetParent: StateID,
    nodeService: NodeService,
) {
    CellsApp.instance.appScope.launch {

        // TODO what do we store/show?
        //   - source files
        //   - target files
        //   - processing
        nodeService.copy(sources, targetParent)
            ?.let {
                withContext(Dispatchers.Main) {
                    showMessage(context, it)
                }
            }
            ?: run {
                withContext(Dispatchers.Main) {
                    showMessage(
                        context,
                        "Launched copy to $targetParent"
                    )
                }
            }
    }
}
