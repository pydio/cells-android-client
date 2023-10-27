package com.pydio.android.cells.ui.core.composables.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.pydio.android.cells.R

@Composable
fun AskForConfirmation(
    icon: ImageVector? = null,
    title: String,
    desc: String,
    confirm: () -> Unit,
    dismiss: () -> Unit,
) {
    AlertDialog(
        icon = {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = "Confirmation Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(25.dp)
                        .padding(bottom = 3.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = { Text(desc) },
        confirmButton = {
            TextButton(onClick = confirm) { Text(stringResource(R.string.button_ok)) }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.button_cancel)) }
        },
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit
        )
    )
}