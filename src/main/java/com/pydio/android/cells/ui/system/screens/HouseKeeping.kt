package com.pydio.android.cells.ui.system.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

@Composable
fun ConfirmClearCache(
    accountID: StateID,
    houseKeepingVM: HouseKeepingVM,
    dismiss: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    AskForConfirmation(
        icon = CellsIcons.EmptyRecycle,
        title = stringResource(R.string.confirm_cache_deletion_title),
        desc = stringResource(R.string.confirm_cache_deletion_message),
        confirm = {
            houseKeepingVM.clearCache(context, accountID)
            dismiss(true)
        },
        dismiss = { dismiss(false) },
    )
}
//
//MaterialAlertDialogBuilder(context)
//.setTitle(R.string.confirm_cache_deletion_title)
//.setIcon(R.drawable.ic_baseline_delete_24)
//.setMessage(context.resources.getString(R.string.confirm_cache_deletion_message))
//.setPositiveButton(R.string.button_confirm) { _, _ ->
//    doClearCache(context, encodedState, nodeService)
//}
//.setNegativeButton(R.string.button_cancel, null)
//.show()
