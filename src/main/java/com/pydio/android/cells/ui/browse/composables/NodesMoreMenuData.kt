package com.pydio.android.cells.ui.browse.composables

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.models.ConnectionState
import com.pydio.android.cells.ui.browse.menus.BookmarksMenu
import com.pydio.android.cells.ui.browse.menus.MultiNodeMenu
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

@Composable
fun NodesMoreMenuData(
    connectionState: ConnectionState,
    type: NodeMoreMenuType,
    subjectIDs: Set<StateID>,
    launch: (NodeAction, Set<StateID>) -> Unit,
) {
    val logTag = "NodeMoreMenuData"

    val treeNodeVM: TreeNodeVM = koinViewModel()

    val nodes: MutableState<Set<RTreeNode>> = remember { mutableStateOf(setOf()) }
    val inRecycle: MutableState<Boolean> = remember { mutableStateOf(false) }
    val containsFolders: MutableState<Boolean> = remember { mutableStateOf(false) }

    LaunchedEffect(key1 = subjectIDs.toString()) {
        Log.d(logTag, "Preparing data for ${subjectIDs.toString()}")
        // Reinitialise values
        val founds = mutableSetOf<RTreeNode>()
        containsFolders.value = false
        inRecycle.value = false
        for (stateID in subjectIDs) {
            if (stateID != StateID.NONE) {
                treeNodeVM.getTreeNode(stateID)?.let { currNode ->
                    founds.add(currNode)
                    if (currNode.isFolder() && !containsFolders.value) {
                        containsFolders.value = true
                    }
                    if (currNode.isInRecycle() && !inRecycle.value) {
                        inRecycle.value = true
                    }
                } ?: { Log.e(logTag, "No node found for $stateID, ignoring") }
            }
        }
        if (founds.isEmpty()) {
            Log.e(logTag, "No node found for the passed selection, aborting")
        } else {
            nodes.value = founds
        }
    }

    if (nodes.value.isNotEmpty()) {
        when (type) {
            NodeMoreMenuType.BOOKMARK -> BookmarksMenu(
                connectionState = connectionState,
                containsFolders = containsFolders.value,
                launch = { launch(it, subjectIDs) },
            )

            NodeMoreMenuType.MORE ->
                MultiNodeMenu(
                    connectionState = connectionState,
                    inRecycle = inRecycle.value,
                    containsFolders = containsFolders.value,
                    launch = { launch(it, subjectIDs) },
                )

            else -> Spacer(modifier = Modifier.height(1.dp))
        }
    }
    // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
    // when no item is defined (This is the case at the beginning when we launch the Side Effect)
    // Log.d(logTag, "## No more menu for $toOpenStateID")
    Spacer(modifier = Modifier.height(1.dp))
}
