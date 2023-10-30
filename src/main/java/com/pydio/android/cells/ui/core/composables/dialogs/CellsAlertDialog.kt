import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.DialogTitle


@Composable
fun CellsAlertDialog(
    icon: ImageVector? = null,
    title: String,
    desc: String,
    confirm: () -> Unit,
    dismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            DialogTitle(
                text = title,
                icon = icon
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
