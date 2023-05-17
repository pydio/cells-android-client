package com.pydio.android.cells.ui.browse.menus

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.browse.models.SingleTransferVM
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.menus.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.menus.SimpleMenuItem
import com.pydio.android.cells.ui.core.composables.modal.ModalBottomSheetState
import com.pydio.android.cells.ui.share.composables.buildStatusString
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// private const val logTag = "TransferMoreMenuData"

enum class TransferMoreMenuType {
    NONE,
    MORE, // <- this one is the default
    SORT_BY,
    FILTER_BY,
}

class TransferMoreMenuState @OptIn(
    ExperimentalMaterial3Api::class
) constructor(
    val type: TransferMoreMenuType,
    val sheetState: ModalBottomSheetState,
    val transferID: Long,
    val openMoreMenu: (TransferMoreMenuType, Long) -> Unit,
    val closeMoreMenu: () -> Unit,
)

@Composable
fun TransferMoreMenu(
    accountID: StateID,
    transferID: Long,
    onClick: (String, Long) -> Unit
) {

    val transferVM: SingleTransferVM = koinViewModel { parametersOf(accountID) }
    val liveItem = transferVM.getTransfer(transferID).collectAsState(null)

    liveItem.value?.let { item ->
        val simpleMenuItems: MutableList<SimpleMenuItem> = mutableListOf()

        if (AppNames.JOB_STATUS_PROCESSING == item.status) {
            simpleMenuItems.add(
                SimpleMenuItem(
                    CellsIcons.Pause,
                    stringResource(id = R.string.pause),
                    onClick = { onClick(AppNames.ACTION_CANCEL, item.transferId) },
                )
            )
        }
        if (AppNames.JOB_STATUS_PAUSED == item.status
            || AppNames.JOB_STATUS_ERROR == item.status
        ) {
            simpleMenuItems.add(
                SimpleMenuItem(
                    CellsIcons.Resume,
                    stringResource(id = R.string.relaunch),
                    onClick = { onClick(AppNames.ACTION_RESTART, item.transferId) },
                )
            )
        }
        if (AppNames.JOB_STATUS_DONE == item.status
            || AppNames.JOB_STATUS_PAUSED == item.status
            || AppNames.JOB_STATUS_ERROR == item.status
        ) {
            simpleMenuItems.add(
                SimpleMenuItem(
                    CellsIcons.Delete,
                    stringResource(id = R.string.delete),
                    onClick = { onClick(AppNames.ACTION_DELETE_RECORD, transferID) },
                )
            )
        }
        simpleMenuItems.add(
            SimpleMenuItem(
                CellsIcons.OpenLocation,
                stringResource(id = R.string.open_parent_in_workspaces),
                onClick = { onClick(AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES, transferID) },
            )
        )

        BottomSheetContent({
            BottomSheetHeader(
                icon = when (item.type) {
                    AppNames.TRANSFER_TYPE_DOWNLOAD -> CellsIcons.DownloadFile
                    else -> CellsIcons.UploadFile
                },
                title = (item.encodedState?.let { StateID.fromId(it).toString() } ?: ""),
                desc = buildStatusString(item).text
            )
        }, simpleMenuItems)


    } ?: run {
        // Log.e(logTag, "######### No item found for #$transferID @ $accountID")
        // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
        // when no item is defined (default case at starting point)
        Spacer(modifier = Modifier.height(1.dp))
    }
}
