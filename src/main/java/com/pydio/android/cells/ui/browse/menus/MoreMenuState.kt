package com.pydio.android.cells.ui.browse.menus

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.pydio.android.cells.ui.browse.models.MoreMenuType
import com.pydio.cells.transport.StateID

class MoreMenuState @OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
) constructor(
    val type: MoreMenuType,
    val sheetState: ModalBottomSheetState,
    val stateID: StateID,
    val openMoreMenu: (MoreMenuType, StateID) -> Unit,
)
