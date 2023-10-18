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
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.menus.BookmarksMenu
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

@Composable
fun NodesMoreMenuData(
    type: NodeMoreMenuType,
    stateIDs: Set<StateID>,
    launch: (NodeAction, Set<StateID>) -> Unit,
) {
    val logTag = "NodeMoreMenuData"

    val treeNodeVM: TreeNodeVM = koinViewModel()
    val items: MutableState<Set<RTreeNode>> = remember { mutableStateOf(setOf()) }
    val workspace: MutableState<RWorkspace?> = remember { mutableStateOf(null) }

    LaunchedEffect(key1 = stateIDs) {
        val founds = mutableSetOf<RTreeNode>()
        for (stateID in stateIDs) {
            if (stateID != StateID.NONE) {
                treeNodeVM.getTreeNode(stateID)?.let { currNode ->
                    founds.add(currNode)
                } ?: { Log.e(logTag, "No node found for $stateID, ignoring") }
            }
        }
        if (founds.isEmpty()) {
            Log.e(logTag, "No node found for the passed selection, aborting")
        } else {
            items.value = founds
        }
    }

    if (items.value.isNotEmpty()) {
        when {
            type == NodeMoreMenuType.BOOKMARK -> BookmarksMenu(
                stateIDs = stateIDs,
                launch = launch,
            )

//                type == NodeMoreMenuType.SEARCH -> SearchMenu(
//                    stateID = toOpenStateID,
//                    rTreeNode = myItem,
//                    launch = { launch(it, toOpenStateID) },
//                )

            // TODO
//                type == NodeMoreMenuType.MORE ->
//                    SingleNodeMenu(
//                        stateID = toOpenStateID,
//                        rTreeNode = myItem,
//                        rWorkspace = workspace.value,
//                        launch = { launch(it, toOpenStateID) },
//                    )

            else -> Spacer(modifier = Modifier.height(1.dp))

        }
    }
    // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
    // when no item is defined (This is the case at the beginning when we launch the Side Effect)
    // Log.d(logTag, "## No more menu for $toOpenStateID")
    Spacer(modifier = Modifier.height(1.dp))
}
