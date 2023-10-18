package com.pydio.android.cells.ui.browse.menus

import androidx.compose.material3.ExperimentalMaterial3Api
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.cells.transport.StateID

class MoreMenuState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val sheetState: ModalBottomSheetState,
    val type: NodeMoreMenuType,
    val stateID: StateID,
    val openMoreMenu: (NodeMoreMenuType, StateID) -> Unit,
)

class SetMoreMenuState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val sheetState: ModalBottomSheetState,
    val type: NodeMoreMenuType,
    val stateIDs: Set<StateID>,
    val openMoreMenu: (NodeMoreMenuType, Set<StateID>) -> Unit,
    val cancelSelection: () -> Unit,
)
