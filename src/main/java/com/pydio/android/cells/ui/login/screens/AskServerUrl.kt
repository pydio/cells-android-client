package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.FormBottomButtons
import com.pydio.android.cells.ui.box.common.FormInput
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.models.NewLoginVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Log
import kotlinx.coroutines.launch

private const val logTag = "AskServerUrl"

@Composable
fun AskServerUrl(
    helper: LoginHelper,
    loginVM: NewLoginVM,
) {

    // Log.e(logTag, "Nav to Login Step")
    val scope = rememberCoroutineScope()
    val isProcessing = loginVM.isProcessing.collectAsState()
    val currAddress = loginVM.serverAddress.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val doPing: (String) -> Unit = { url ->
        // TODO add sanity checks
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
        setUrl = { loginVM.setAddress(it) },
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

    val localFocusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imeAction = ImeAction.Done

    val keyboardActions = KeyboardActions(
        onNext = { localFocusManager.moveFocus(FocusDirection.Down) },
        onDone = {
            pingUrl(urlString)
            localFocusManager.clearFocus()
            keyboardController?.hide()
        }
    )

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
            modifier = Modifier.fillMaxWidth(),
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
    CellsTheme {
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
