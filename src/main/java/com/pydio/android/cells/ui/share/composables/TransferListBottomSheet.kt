package com.pydio.android.cells.ui.share

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.ui.core.composables.BottomSheetContent
import com.pydio.android.cells.ui.core.composables.BottomSheetHeader
import com.pydio.android.cells.ui.core.composables.SimpleMenuItem
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

@Composable
fun TransferBottomSheet(
    item: RTransfer?,
    onClick: (String, Long) -> Unit
) {
    if (item == null) {
        // Prevent this error: java.lang.IllegalArgumentException: The initial value must have an associated anchor.
        // when no item is defined (default case at starting point)
        Spacer(modifier = Modifier.height(1.dp))
        return
    }

    val simpleMenuItems: MutableList<SimpleMenuItem> = mutableListOf()

    if (AppNames.JOB_STATUS_PROCESSING == item.status) {
        simpleMenuItems.add(
            SimpleMenuItem(
                CellsIcons.Pause,
                stringResource(id = R.string.pause)
            ) { onClick(AppNames.ACTION_CANCEL, item.transferId) },
        )
    }
    if (AppNames.JOB_STATUS_CANCELLED == item.status
        || AppNames.JOB_STATUS_ERROR == item.status
    ) {
        simpleMenuItems.add(
            SimpleMenuItem(
                CellsIcons.Resume,
                stringResource(id = R.string.relaunch)
            ) { onClick(AppNames.ACTION_RESTART, item.transferId) },
        )
    }
    if (AppNames.JOB_STATUS_DONE == item.status
        || AppNames.JOB_STATUS_CANCELLED == item.status
        || AppNames.JOB_STATUS_ERROR == item.status
    ) {
        simpleMenuItems.add(
            SimpleMenuItem(
                CellsIcons.Delete,
                stringResource(id = R.string.delete)
            ) { onClick(AppNames.ACTION_DELETE_RECORD, item.transferId) },
        )
    }
    simpleMenuItems.add(
        SimpleMenuItem(
            CellsIcons.OpenLocation,
            stringResource(id = R.string.open_parent_in_workspaces)
        ) { onClick(AppNames.ACTION_OPEN_PARENT_IN_WORKSPACES, item.transferId) },
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
}


@Preview(showBackground = true)
@Composable
fun TransferBottomSheetPreview() {
    val context = LocalContext.current
    val onClick: (String) -> Unit = { title ->
        Toast.makeText(
            context, title, Toast.LENGTH_SHORT
        ).show()
    }
    val simpleMenuItems: List<SimpleMenuItem> = listOf(
        SimpleMenuItem(CellsIcons.Share, "Share") { onClick("Share") },
        SimpleMenuItem(CellsIcons.Link, "Get Link") { onClick("Get Link") },
        SimpleMenuItem(CellsIcons.Edit, "Edit") { onClick("Edit") },
        SimpleMenuItem(CellsIcons.Delete, "Delete") { onClick("Delete") },
    )

    BottomSheetContent(
        {
            BottomSheetHeader(
                icon = CellsIcons.Processing,
                title = "My Transfer of jpg.pdf",
                desc = "45MB, started at 5.54 AM, 46% done"
            )
        },
        simpleMenuItems
    )
}
