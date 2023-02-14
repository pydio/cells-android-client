package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.FormBottomButtons
import com.pydio.android.cells.ui.box.common.FormInput
import com.pydio.android.cells.ui.login.DoneRoute
import com.pydio.android.cells.ui.login.LoginViewModelNew
import com.pydio.android.cells.ui.login.nav.StateViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.launch

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
    launchP8Auth: (String, String, String?) -> Unit,
) {

    val localFocusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val loginImeAction = ImeAction.Next
    val pwdImeAction = if (hasCaptcha) ImeAction.Next else ImeAction.Done
    val captchaImeAction = ImeAction.Done

    val keyboardActions = KeyboardActions(
        onNext = { localFocusManager.moveFocus(FocusDirection.Down) },
        onDone = {
            launchP8Auth(loginString, pwdString, captchaString)
            localFocusManager.clearFocus()
            keyboardController?.hide()
        }
    )

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
            modifier = Modifier.fillMaxWidth(),
            errorMessage = "",
            imeAction = loginImeAction,
            keyboardActions = keyboardActions
        )

        FormInput(
            value = pwdString,
            description = "Password",
            onValueChanged = { updatePwd(it) },
            isProcessing = isProcessing,
            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                errorMessage = errMsg,
                imeAction = captchaImeAction,
                keyboardActions = keyboardActions
            )
        }

        FormBottomButtons(
            backBtnLabel = stringResource(R.string.button_back),
            back = { goBack() },
            nextBtnLabel = stringResource(id = R.string.button_next),
            next = { launchP8Auth(loginString, pwdString, captchaString) },
            isProcessing = isProcessing,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun P8Credentials(
    stateVM: StateViewModel,
    loginVM: LoginViewModelNew,
    navigateTo: (String?) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val isProcessing = loginVM.isProcessing.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val loginString = rememberSaveable { mutableStateOf("") }
    val updateLogin: (String) -> Unit = { loginString.value = it.trim() }
    val pwdString = rememberSaveable { mutableStateOf("") }
    val updatePwd: (String) -> Unit = { pwdString.value = it }

    val launchP8Auth: (String, String, String?) -> Unit = { login, pwd, captcha ->
        scope.launch {
            val res = loginVM.logToP8(login, pwd, captcha)
            if (res) {
                navigateTo(DoneRoute.route)
            } // else do nothing: error message has already been displayed and we stay on the page
        }
    }

    P8Credentials(
        isProcessing.value,
        loginString.value,
        updateLogin,
        pwdString.value,
        updatePwd,
        message = message.value,
        errMsg = errMsg.value,
        goBack = { navigateTo(null) },
        launchP8Auth = launchP8Auth,
    )
}

@Preview(name = "P8Credentials Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "P8Credentials Dark"
)
@Composable
private fun P8CredentialsPreview() {
    CellsTheme {
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
            { _, _, _ -> },
        )
    }
}
