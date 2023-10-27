package com.pydio.android.cells.ui.system.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.dialogs.AskForConfirmation
import com.pydio.android.cells.ui.system.models.HouseKeepingVM
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConfirmClearCache(
    accountID: StateID,
    houseKeepingVM: HouseKeepingVM = koinViewModel(),
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
