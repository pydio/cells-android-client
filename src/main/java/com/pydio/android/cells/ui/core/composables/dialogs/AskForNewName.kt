package com.pydio.android.cells.ui.core.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.DialogTitle
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.cells.transport.StateID

@Composable
fun AskForNewName(
    srcID: StateID,
    rename: (srcID: StateID, name: String) -> Unit,
    dismiss: () -> Unit
) {
    val newName = remember {
        mutableStateOf(srcID.fileName)
    }

    val updateValue: (String) -> Unit = { newName.value = it }

    AlertDialog(
        title = {
            DialogTitle(icon = CellsIcons.Edit, text = stringResource(R.string.rename_dialog_title))
        },
        text = { AskForNewNameContent(srcID.fileName, newName.value, updateValue) },
        confirmButton = {
            TextButton(
                onClick = {
                    rename(srcID, newName.value)
                }
            ) { Text(stringResource(R.string.rename_dialog_confirm_button)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    dismiss()
                }
            ) { Text(stringResource(R.string.button_cancel)) }
        },
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskForNewNameContent(oldName: String, value: String, updateValue: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.rename_dialog_message, oldName))
        Divider()
        // TODO pre-select the part of the text before the extension
        TextField(
            value = value,
            onValueChange = { updateValue(it) },
        )
    }
}
