package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
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
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.launch

@Composable
fun P8Credentials(
    stateID: StateID,
    skipVerify: Boolean,
    helper: LoginHelper,
    loginVM: LoginVM,
) {

    val scope = rememberCoroutineScope()
    val isProcessing = loginVM.isProcessing.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    // This might have been initialised when re-logging a P8 account
    val username = rememberSaveable {
        mutableStateOf(stateID.username ?: "")
    }
    val setUsername: (String) -> Unit = {
        username.value = it.trim()
    }
    val pwd = rememberSaveable {
        mutableStateOf("")
    }
    val setPwd: (String) -> Unit = {
        pwd.value = it
    }

    P8Credentials(
        isProcessing.value,
        username.value,
        setUsername,
        pwd.value,
        setPwd,
        message = message.value,
        errMsg = errMsg.value,
        goBack = { scope.launch { helper.back() } },
        launchP8Auth = {
            scope.launch {
                helper.launchP8Auth(stateID.serverUrl, skipVerify, username.value, pwd.value, null)
            }
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun P8Credentials(
    isProcessing: Boolean,
    loginString: String,
    updateLogin: (String) -> Unit,
    pwdString: String,
    updatePwd: (String) -> Unit,
    // TODO reimplement this
    captchaString: String? = null,
    updateCaptcha: (String) -> Unit = {},
    hasCaptcha: Boolean = false,
    message: String?,
    errMsg: String?,
    goBack: () -> Unit,
    launchP8Auth: () -> Unit,
) {

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val loginImeAction = ImeAction.Next
    val pwdImeAction = if (hasCaptcha) ImeAction.Next else ImeAction.Done
    val captchaImeAction = ImeAction.Done

    val onDone: () -> Unit = {
        launchP8Auth()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val keyboardActions = KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) },
        onDone = { onDone() }
    )

    val modifier = Modifier
        .fillMaxWidth()
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

    DefaultLoginPage(
        isProcessing = isProcessing,
        title = stringResource(R.string.p8_credentials_title),
        desc = stringResource(R.string.p8_credentials_desc),
        message = message
    ) {

        FormInput(
            value = loginString,
            description = "Login",
            //errorMessage = errMsg,
            // We only display the error message on the password
            onValueChanged = { updateLogin(it) },
            isProcessing = isProcessing,
            modifier = modifier,
            errorMessage = "",
            imeAction = loginImeAction,
            keyboardActions = keyboardActions
        )

        FormInput(
            value = pwdString,
            description = "Password",
            onValueChanged = { updatePwd(it) },
            isProcessing = isProcessing,
            modifier = modifier,
            isPassword = true,
            errorMessage = errMsg,
            pwdImeAction,
            keyboardActions
        )

        if (hasCaptcha) { // Never true for the time being.
            FormInput(
                value = captchaString ?: "",
                description = "Captcha",
                onValueChanged = { updateCaptcha(it) },
                isProcessing = isProcessing,
                modifier = modifier,
                errorMessage = errMsg,
                imeAction = captchaImeAction,
                keyboardActions = keyboardActions
            )
        }

        FormBottomButtons(
            backBtnLabel = stringResource(R.string.button_back),
            back = { goBack() },
            nextBtnLabel = stringResource(id = R.string.button_next),
            next = { launchP8Auth() },
            isProcessing = isProcessing,
        )
    }
}


@Preview(name = "P8Credentials Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "P8Credentials Dark"
)
@Composable
private fun P8CredentialsPreview() {
    UseCellsTheme {
        P8Credentials(
            isProcessing = true,
            "john",
            {},
            "password",
            {},
            "captcha",
            {},
            false,
            null,
            null,
            { },
            { },
        )
    }
}
