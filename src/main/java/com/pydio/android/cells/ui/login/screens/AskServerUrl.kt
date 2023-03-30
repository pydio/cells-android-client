package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import android.util.Log
import android.view.KeyEvent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.FormBottomButtons
import com.pydio.android.cells.ui.core.composables.FormInput
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.theme.UseCellsTheme
import kotlinx.coroutines.launch

private const val logTag = "AskServerUrl"

@Composable
fun AskServerUrl(
    helper: LoginHelper,
    loginVM: LoginVM,
) {

    // Log.e(logTag, "Nav to Login Step")
    val scope = rememberCoroutineScope()
    val isProcessing = loginVM.isProcessing.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val currAddress = rememberSaveable() {
        mutableStateOf("https://")
    }
    val setUrl: (String) -> Unit = {
        currAddress.value = it.lowercase().trim()
    }

    val doPing: (String) -> Unit = { url ->
        scope.launch {
            val res = loginVM.pingAddress(url, false)
            Log.e(logTag, "After ping with no SkipVerify flag, res: $res")
            res?.let { helper.afterPing(it) }
        }
    }

    AskServerUrl(
        isProcessing = isProcessing.value,
        message = message.value,
        errMsg = errMsg.value,
        urlString = currAddress.value,
        setUrl = setUrl,
        pingUrl = { doPing(currAddress.value) },
        cancel = { helper.cancel() }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AskServerUrl(
    isProcessing: Boolean,
    message: String?,
    errMsg: String?,
    urlString: String,
    setUrl: (String) -> Unit,
    pingUrl: (String) -> Unit,
    cancel: () -> Unit,
) {

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imeAction = ImeAction.Done

    val onDone: () -> Unit = {
        pingUrl(urlString)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) },
        onDone = { onDone() }
    )

    val modifier = Modifier.onPreviewKeyEvent {
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

    DefaultLoginPage(
        isProcessing = isProcessing,
        title = stringResource(R.string.ask_url_title),
        desc = stringResource(R.string.ask_url_desc),
        message = message
    ) {

        FormInput(
            value = urlString,
            description = "Server URL",
            onValueChanged = { setUrl(it) },
            isProcessing = isProcessing,
            modifier = modifier.fillMaxWidth(),
            errorMessage = errMsg,
            imeAction = imeAction,
            keyboardActions = keyboardActions
        )

        FormBottomButtons(
            backBtnLabel = stringResource(id = R.string.button_cancel),
            back = { cancel() },
            nextBtnLabel = stringResource(id = R.string.button_next),
            next = { pingUrl(urlString) },
            isProcessing = isProcessing,
        )
    }
}

@Preview(name = "AskUrl Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "AskUrl Dark"
)
@Composable
private fun AskUrlPreview() {
    UseCellsTheme {
        AskServerUrl(
            isProcessing = true,
            message = "pinging server",
            errMsg = null,
            urlString = "https://www.example.com",
            setUrl = { },
            pingUrl = { },
            cancel = { },
        )
    }
}
