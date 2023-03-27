package com.pydio.android.cells.ui.core.composables.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
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
import com.pydio.cells.transport.StateID

@Composable
fun AskForFolderName(
    parStateID: StateID,
    createFolderAt: (parStateID: StateID, name: String) -> Unit,
    dismiss: () -> Unit
) {
    val folderName = remember {
        mutableStateOf("")
    }

    val updateValue: (String) -> Unit = { folderName.value = it }

    AlertDialog(
        title = { Text(stringResource(R.string.dialog_create_folder_title)) },
        text = { AskForNameContent(folderName.value, updateValue) },
        confirmButton = {
            TextButton(
                onClick = {
                    // Toast.makeText(ctx, "OK button clicked", Toast.LENGTH_LONG).show()
                    createFolderAt(parStateID, folderName.value)
                }
            ) { Text(stringResource(R.string.dialog_create_folder_positive_btn)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    // Toast.makeText(ctx, "Cancel button clicked!!", Toast.LENGTH_LONG).show()
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
private fun AskForNameContent(value: String, updateValue: (String) -> Unit) {
    Column {
        TextField(
            value = value,
            onValueChange = { updateValue(it) },
            supportingText = {
                Text(stringResource(R.string.dialog_create_folder_support_text))
            },
        )
    }
}
