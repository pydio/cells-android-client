package com.pydio.android.cells.ui.browse.menus

import androidx.compose.material3.ExperimentalMaterial3Api
import com.pydio.android.cells.ui.browse.composables.NodeMoreMenuType
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.cells.transport.StateID

class MoreMenuState @OptIn(ExperimentalMaterial3Api::class) constructor(
    val type: NodeMoreMenuType,
    val sheetState: ModalBottomSheetState,
    val stateID: StateID,
    val openMoreMenu: (NodeMoreMenuType, StateID) -> Unit,
)
