package com.pydio.android.cells.ui.browse.menus

import androidx.compose.material3.ExperimentalMaterial3Api
import com.pydio.android.cells.ui.browse.composables.TransferMoreMenuType
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState

class TransferMoreMenuState @OptIn(
    ExperimentalMaterial3Api::class
) constructor(
    val type: TransferMoreMenuType,
    val sheetState: ModalBottomSheetState,
    val transferID: Long,
    val openMoreMenu: (TransferMoreMenuType, Long) -> Unit,
    val closeMoreMenu: () -> Unit,
)
