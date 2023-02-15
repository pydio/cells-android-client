package com.pydio.android.cells.ui.box.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.AuthApp
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Str

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormInput(
    value: String,
    description: String,
    onValueChanged: (String) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = dimensionResource(R.dimen.form_input_horizontal_padding),
            end = dimensionResource(R.dimen.form_input_horizontal_padding),
            top = dimensionResource(R.dimen.form_input_vertical_padding),
            bottom = dimensionResource(R.dimen.form_input_vertical_padding),
        ),
    isPassword: Boolean = false,
    errorMessage: String?,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        label = { Text(text = description) },
        supportingText = {
            if (Str.notEmpty(errorMessage)) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        },
        enabled = !isProcessing,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = keyboardActions,
        onValueChange = { newValue -> onValueChanged(newValue) },
        modifier = modifier,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}

@Composable
fun FormBottomButtons(
    backBtnLabel: String,
    back: () -> Unit,
    nextBtnLabel: String,
    next: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
    isBackPrimary: Boolean = false,
) {

    val btnMod = Modifier
        .padding(all = dimensionResource(R.dimen.margin))
        .wrapContentWidth(Alignment.CenterHorizontally)

    Row(modifier = modifier) {

        if (isBackPrimary) {
            Button(
                onClick = back,
                modifier = btnMod.weight(.5f),
                enabled = !isProcessing,
            ) {
                Text(backBtnLabel)
            }

            OutlinedButton(
                onClick = next,
                modifier = btnMod.weight(.5f),
                enabled = !isProcessing,
            ) {
                Text(nextBtnLabel)
            }
        } else {
            OutlinedButton(
                onClick = back,
                modifier = btnMod.weight(.5f),
                enabled = !isProcessing,
            ) {
                Text(backBtnLabel)
            }
            Button(
                onClick = next,
                modifier = btnMod.weight(.5f),
                enabled = !isProcessing,
            ) {
                Text(nextBtnLabel)
            }
        }
    }
}

@Preview(name = "Default FormInput Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Default FormInput Dark"
)
@Composable
private fun FormInputPreview() {
    CellsTheme {
        Column(Modifier.fillMaxWidth()) {
            FormInput(
                value = "https://files.example.com:666",
                description = "Server URL",
                onValueChanged = { },
                isProcessing = false,
                modifier = Modifier.fillMaxWidth(),
                errorMessage = null,
                imeAction = ImeAction.Default,
                keyboardActions = KeyboardActions.Default
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Default FormInput Dark with Error"
)
@Composable
private fun FormInputErrorPreview() {
    val dummyURL = "https://files.example.com:666"
    CellsTheme {
        Column(Modifier.fillMaxWidth()) {
            FormInput(
                value = dummyURL,
                description = "Server URL",
                onValueChanged = { },
                isProcessing = true,
                errorMessage = "Cannot reach server at $dummyURL",
                imeAction = ImeAction.Default,
                keyboardActions = KeyboardActions.Default
            )
        }
    }
}

@Preview(name = "Btns Light Mode")
@Composable
private fun FormBottomButtonsPreview() {
    CellsTheme {
        AuthApp {
            Column(Modifier.fillMaxWidth()) {
                FormBottomButtons(
                    backBtnLabel = "Cancel",
                    back = { },
                    nextBtnLabel = "Next",
                    next = { },
                    isProcessing = false,
                    isBackPrimary = false
                )
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun FormBottomButtons2Preview() {
    CellsTheme {
        AuthApp {
            Column(Modifier.fillMaxWidth()) {
                FormBottomButtons(
                    backBtnLabel = "Go Back (recommended)",
                    back = { },
                    nextBtnLabel = "Accept the risk and continue",
                    next = { },
                    isProcessing = false,
                    isBackPrimary = true,
                )
            }
        }
    }
}