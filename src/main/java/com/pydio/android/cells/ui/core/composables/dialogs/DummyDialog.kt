package com.pydio.android.cells.ui.core.composables.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.pydio.android.cells.R

@Composable
fun DummyDialog(
    dismiss: () -> Unit
) {
    val ctx = LocalContext.current

    AlertDialog(
        title = { Text("Title") },
        text = { DummyDialogContent("A test for the dialogs.") },
        confirmButton = {
            TextButton(
                onClick = {
                    Toast.makeText(ctx, "OK button clicked", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            ) { Text(stringResource(R.string.button_ok)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    Toast.makeText(ctx, "Cancel button clicked", Toast.LENGTH_LONG).show()
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

@Composable
private fun DummyDialogContent(desc: String = "a description") {
    val txtValue = remember {
        mutableStateOf("")
    }
    Column {
        Text(desc)
        Divider()
        TextField(
            value = txtValue.value,
            onValueChange = { txtValue.value = it },
            supportingText = {
                Text("Please, test the new dialog")
            },
        )
    }
}
