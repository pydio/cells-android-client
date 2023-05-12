package com.pydio.android.cells.ui.core.composables.dialogs

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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

    val doCreate: () -> Unit = { createFolderAt(parStateID, folderName.value) }

    AlertDialog(
        title = { Text(stringResource(R.string.dialog_create_folder_title)) },
        text = { AskForNameContent(folderName.value, updateValue, doCreate) },
        confirmButton = {
            TextButton(onClick = doCreate) { Text(stringResource(R.string.dialog_create_folder_positive_btn)) }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.button_cancel)) }
        },
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit
        )
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AskForNameContent(
    value: String,
    updateValue: (String) -> Unit,
    doCreate: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imeAction = ImeAction.Done

    val onDone: () -> Unit = {
        doCreate()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) },
        onDone = { onDone() }
    )

    val focusRequester = FocusRequester()

    val modifier = Modifier
        .focusRequester(focusRequester)
        .onPreviewKeyEvent {
            if (it.key == Key.Tab && it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                focusManager.moveFocus(FocusDirection.Down)
                true
            } else if (it.key == Key.Enter) {
                onDone()
                true
            } else {
                false
            }
        }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { updateValue(it) },
            modifier = modifier,
            label = {
                Text(stringResource(R.string.name_label))
            },
            supportingText = {
                Text(stringResource(R.string.dialog_create_folder_support_text))
            },
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = keyboardActions,
        )
    }

    LaunchedEffect(key1 = true) {
        focusRequester.requestFocus()
    }
}
