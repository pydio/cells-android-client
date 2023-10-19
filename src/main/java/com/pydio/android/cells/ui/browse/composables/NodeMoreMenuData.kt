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
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.accounts.RWorkspace
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.menus.BookmarkMenu
import com.pydio.android.cells.ui.browse.menus.CreateOrImportMenu
import com.pydio.android.cells.ui.browse.menus.OfflineMenu
import com.pydio.android.cells.ui.browse.menus.RecycleMenu
import com.pydio.android.cells.ui.browse.menus.RecycleParentMenu
import com.pydio.android.cells.ui.browse.menus.SearchMenu
import com.pydio.android.cells.ui.browse.menus.SingleNodeMenu
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

enum class NodeMoreMenuType {
    NONE, MORE, SEARCH, OFFLINE, BOOKMARK, CREATE, SORT_BY,
}

@Composable
fun NodeMoreMenuData(
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    launch: (NodeAction, StateID) -> Unit,
) {
    val logTag = "NodeMoreMenuData"

    val treeNodeVM: TreeNodeVM = koinViewModel()
    val item: MutableState<RTreeNode?> = remember { mutableStateOf(null) }
    val workspace: MutableState<RWorkspace?> = remember { mutableStateOf(null) }

    LaunchedEffect(key1 = toOpenStateID) {
        if (toOpenStateID != StateID.NONE) {
            treeNodeVM.getTreeNode(toOpenStateID)?.let { currNode ->
                item.value = currNode
            } ?: { Log.e(logTag, "No node found for $toOpenStateID, aborting") }

            if (toOpenStateID.isWorkspaceRoot) {
                treeNodeVM.getWS(toOpenStateID)?.let { currNode ->
                    workspace.value = currNode
                }
            }
        }
    }

    if (type == NodeMoreMenuType.SORT_BY) {
        SortByMenu(
            type = ListType.DEFAULT,
            done = { launch(NodeAction.SortBy, toOpenStateID) },
        )
    } else if (toOpenStateID.slug != null) {
        item.value?.let { myItem ->
            when {
                myItem.isRecycle() -> RecycleParentMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = { launch(it, toOpenStateID) },
                )

                myItem.isInRecycle() -> RecycleMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = { launch(it, toOpenStateID) },
                )

                type == NodeMoreMenuType.CREATE -> CreateOrImportMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    rWorkspace = workspace.value,
                    launch = { launch(it, toOpenStateID) },
                )

                type == NodeMoreMenuType.OFFLINE -> OfflineMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = { launch(it, toOpenStateID) },
                )

                type == NodeMoreMenuType.BOOKMARK -> BookmarkMenu(
                    treeNodeVM = treeNodeVM,
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                )

                type == NodeMoreMenuType.SEARCH -> SearchMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = { launch(it, toOpenStateID) },
                )

                type == NodeMoreMenuType.MORE ->
                    SingleNodeMenu(
                        stateID = toOpenStateID,
                        rTreeNode = myItem,
                        rWorkspace = workspace.value,
                        launch = { launch(it, toOpenStateID) },
                    )

                else -> Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
    // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
    // when no item is defined (This is the case at the beginning when we launch the Side Effect)
    // Log.d(logTag, "## No more menu for $toOpenStateID")
    Spacer(modifier = Modifier.height(1.dp))
}
