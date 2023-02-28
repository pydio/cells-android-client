package com.pydio.android.cells.ui.browse.models

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
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.ui.browse.composables.BookmarkMoreMenuView
import com.pydio.android.cells.ui.browse.composables.CreateMenuView
import com.pydio.android.cells.ui.browse.composables.NodeAction
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuView
import com.pydio.android.cells.ui.browse.composables.OfflineMoreMenuView
import com.pydio.android.cells.ui.browse.composables.RecycleMoreMenuView
import com.pydio.android.cells.ui.browse.composables.RecycleParentMoreMenuView
import com.pydio.cells.transport.StateID

private const val logTag = "NodeMoreMenuData"

enum class MoreMenuType {
    NONE,
    MORE,
    OFFLINE,
    BOOKMARK,
    CREATE,
}

@Composable
fun NodeMoreMenuData(
    type: MoreMenuType,
    toOpenStateID: StateID?,
    launch: (NodeAction) -> Unit,
    moreMenuVM: MoreMenuVM,
    tint: Color,
    bgColor: Color,
) {
    val item: MutableState<RTreeNode?> = remember {
        mutableStateOf(null)
    }

    LaunchedEffect(key1 = toOpenStateID) {
        toOpenStateID?.let {
            val currNode = moreMenuVM.getTreeNode(it) ?: run {
                Log.e(logTag, "No node found for $it, aborting")
                // actionDone() TODO do something?
                null
            }
            Log.e(
                logTag,
                "## After effect, treeNode $currNode for ${currNode?.getStateID() ?: "NaN"}"
            )
            item.value = currNode
        }
    }

    // We have to provide early a dummy content when not enough data to build a menu is present.
    if (toOpenStateID != null && toOpenStateID.parentPath != null && item.value != null) {
        val myItem = item.value!!
        Log.e(logTag, "## ABOUT TO COMPOSE FOR $myItem, ${myItem.getStateID()}")

        when {
            myItem.isRecycle() -> RecycleParentMoreMenuView(
                stateID = toOpenStateID,
                rTreeNode = myItem,
                launch = launch,
                tint = tint,
                bgColor = bgColor,
            )
            myItem.isInRecycle() -> RecycleMoreMenuView(
                stateID = toOpenStateID,
                rTreeNode = myItem,
                launch = launch,
                tint = tint,
                bgColor = bgColor,
            )
            type == MoreMenuType.CREATE -> CreateMenuView(
                stateID = toOpenStateID,
                rTreeNode = myItem,
                launch = launch,
                tint = tint,
                bgColor = bgColor,
            )
            type == MoreMenuType.OFFLINE -> OfflineMoreMenuView(
                stateID = toOpenStateID,
                rTreeNode = myItem,
                launch = launch,
                tint = tint,
                bgColor = bgColor,
            )
            type == MoreMenuType.BOOKMARK -> BookmarkMoreMenuView(
                stateID = toOpenStateID,
                rTreeNode = myItem,
                launch = launch,
                tint = tint,
                bgColor = bgColor,
            )
            else ->
                NodeMoreMenuView(
                    stateID = toOpenStateID,
                    rTreeNode = myItem,
                    launch = launch,
                    tint = tint,
                    bgColor = bgColor,
                )
        }
    } else {
        // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
        // when no item is defined (This is the case at the beginning when we launch the Side Effect)
        Log.d(logTag, "## No more menu for $toOpenStateID")
        Spacer(modifier = Modifier.height(1.dp))
    }
}
