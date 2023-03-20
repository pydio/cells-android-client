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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.menus.BookmarkMenu
import com.pydio.android.cells.ui.browse.menus.CreateOrImportMenu
import com.pydio.android.cells.ui.browse.menus.OfflineMenu
import com.pydio.android.cells.ui.browse.menus.RecycleMenu
import com.pydio.android.cells.ui.browse.menus.RecycleParentMenu
import com.pydio.android.cells.ui.browse.menus.SingleNodeMenu
import com.pydio.android.cells.ui.browse.menus.SortByMenu
import com.pydio.android.cells.ui.browse.models.TreeNodeVM
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val logTag = "NodeMoreMenuData"

enum class NodeMoreMenuType {
    NONE,
    MORE, // <- this one is the default
    SEARCH,
    OFFLINE,
    BOOKMARK,
    CREATE,
    SORT_BY,
}

@Composable
fun NodeMoreMenuData(
    type: NodeMoreMenuType,
    toOpenStateID: StateID,
    launch: (NodeAction) -> Unit,
    tint: Color,
    bgColor: Color,
) {
    val item: MutableState<RTreeNode?> = remember {
        mutableStateOf(null)
    }

    val stateID: StateID = toOpenStateID ?: StateID.NONE
    val moreMenuVM: TreeNodeVM = koinViewModel(parameters = { parametersOf(stateID) })

    LaunchedEffect(key1 = toOpenStateID) {
        toOpenStateID?.let {
            if (it != StateID.NONE) {
                moreMenuVM.getTreeNode(it)?.let { currNode ->
                    item.value = currNode
                } ?: {
                    Log.e(logTag, "No node found for $it, aborting")
                    // actionDone() TODO do something?
                }
            }
        }
    }

    // Log.d(logTag, "## In Node More Menu data for $toOpenStateID ")

    // We have to provide early a dummy content when not enough data to build a menu is present.
    if (toOpenStateID != null && toOpenStateID.workspace != null && toOpenStateID != StateID.NONE) {

        item.value?.let { myItem ->

            Log.e(logTag, "## Choosing MenuView with type $type for $toOpenStateID }")

            when {
                myItem.isRecycle() -> RecycleParentMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
                myItem.isInRecycle() -> RecycleMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
                type == NodeMoreMenuType.CREATE -> CreateOrImportMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
                type == NodeMoreMenuType.OFFLINE -> OfflineMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
                type == NodeMoreMenuType.BOOKMARK -> BookmarkMenu(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
                type == NodeMoreMenuType.SORT_BY -> SortByMenu(
                    type = ListType.DEFAULT,
                    done = { launch(NodeAction.SortBy) },
                    tint = tint,
                    bgColor = bgColor,
                )
                type == NodeMoreMenuType.MORE ->
                    SingleNodeMenu(
                        stateID = toOpenStateID,
                        rTreeNode = myItem,
                        launch = launch,
                        tint = tint,
                        bgColor = bgColor,
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
