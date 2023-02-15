package com.pydio.android.cells.ui.box.account

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.FormBottomButtons
import com.pydio.android.cells.ui.box.common.FormInput
import com.pydio.android.cells.ui.core.composables.TitleDescColumnBloc
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Str

private const val logTag = "DefaultLoginPage.kt"

//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun AskServerUrl(loginVM: LoginViewModelNew) {
//
//    // Log.e(logTag, "Nav to Login Step")
//    val scope = rememberCoroutineScope()
//    val isProcessing = loginVM.isProcessing.collectAsState()
//    val currAddress = loginVM.serverAddress.collectAsState()
//    val message = loginVM.message.collectAsState()
//    val errMsg = loginVM.errorMessage.collectAsState()
//
//    val doPing: (String) -> Unit = { url ->
//        // TODO add sanity checks
//        scope.launch {
//            loginVM.pingAddress()
//        }
//    }
//
//    AskServerUrl(
//        isProcessing = isProcessing.value,
//        message = message.value,
//        errMsg = errMsg.value,
//        urlString = currAddress.value,
//        setUrl = { loginVM.setAddress(it) },
//        pingUrl = { doPing(currAddress.value) },
//        cancel = { /*afterAuth(false)*/ }
//    )
//
//}


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
        onNext = { localFocusManager.moveFocus(FocusDirection.Companion.Down) },
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

//@Composable
//fun SkipVerify(loginVM: LoginVM) {
//
//    Log.e(logTag, "Nav to Login Step")
//
//    val scope = rememberCoroutineScope()
//    val isProcessing = loginVM.isProcessing.collectAsState()
//    val currAddress = loginVM.serverAddress.collectAsState()
//    val message = loginVM.message.collectAsState()
//    val errMsg = loginVM.errorMessage.collectAsState()
//
//    val doPing: (String) -> Unit = { url ->
//        // TODO add sanity checks
//        scope.launch {
//            loginVM.pingAddress()
//        }
//    }
//
//    SkipVerify(
//        isProcessing.value,
//        currAddress.value,
//        message.value,
//        errMsg.value,
//        goBack = { loginVM.navigateBack() },
//        accept = { scope.launch { loginVM.confirmSkipVerifyAndPing() } },
//    )
//}

//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun P8Credentials(loginVM: LoginViewModelNew) {
//
//    val scope = rememberCoroutineScope()
//    val isProcessing = loginVM.isProcessing.collectAsState()
//    val message = loginVM.message.collectAsState()
//    val errMsg = loginVM.errorMessage.collectAsState()
//
//    val loginString = rememberSaveable { mutableStateOf("") }
//    val updateLogin: (String) -> Unit = { loginString.value = it }
//    val pwdString = rememberSaveable { mutableStateOf("") }
//    val updatePwd: (String) -> Unit = { pwdString.value = it }
//
//    val launchP8Auth: (String, String, String?) -> Unit = {
//        // TODO add validation
//            login, pwd, captcha ->
//        scope.launch { loginVM.logToP8(login, pwd, captcha) }
//    }
//
//    P8Credentials(
//        isProcessing.value,
//        loginString.value,
//        updateLogin,
//        pwdString.value,
//        updatePwd,
//        message = message.value,
//        errMsg = errMsg.value,
//        goBack = { /*navController.navigateBack()*/ },
//        launchP8Auth = launchP8Auth,
//    )
//}

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
        onNext = { localFocusManager.moveFocus(FocusDirection.Companion.Down) },
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


@Composable
fun SkipVerify(
    isProcessing: Boolean,
    fqdn: String,
    message: String?,
    errMsg: String?,
    goBack: () -> Unit,
    accept: () -> Unit,
) {

    val (msg, isError) = if (Str.notEmpty(errMsg)) {
        Pair(errMsg, true)
    } else {
        Pair(message, false)
    }

    DefaultLoginPage(
        isProcessing = isProcessing,
        title = stringResource(R.string.confirm_skip_verify_title),
        desc = null,
        message = msg,
        isErrorMsg = isError,
    ) {

        val btnMod = Modifier
            .padding(all = dimensionResource(R.dimen.margin))
            .wrapContentWidth(Alignment.CenterHorizontally)

        Text(
            text = stringResource(R.string.confirm_skip_verify_desc, fqdn),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
        )

        Row {
            TextButton(
                onClick = goBack,
                modifier = btnMod.weight(.5f),
            ) {
                Text(stringResource(R.string.confirm_skip_verify_cancel))
            }

            TextButton(
                onClick = accept,
                modifier = btnMod.weight(.5f),
            ) {
                Text(stringResource(R.string.confirm_skip_verify_accept))
            }
        }
    }
}

@Composable
fun ProcessAuth(
    isProcessing: Boolean,
    message: String?,
) {

    DefaultLoginPage(
        isProcessing = isProcessing,
        title = "Processing...",
        desc = stringResource(R.string.oauth_code_flow_desc),
        message = message,
    ) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .size(240.dp)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
        ) {
            Image(
                painterResource(R.drawable.pydio_logo),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun AfterSuccessfulLogin() {

    DefaultLoginPage(
        isProcessing = true,
        title = "Success",
        desc = null,
        message = "You are now connected. Redirecting.",
    ) {
        Surface(
            modifier = Modifier
                .size(240.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Image(
                painterResource(R.drawable.pydio_logo),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                contentDescription = null
            )
        }
    }
}

@Composable
fun DefaultLoginPage(
    isProcessing: Boolean,
    title: String,
    desc: String?,
    message: String?,
    isErrorMsg: Boolean = false,
    Content: @Composable () -> Unit,
) {
    Log.e(logTag, "Recomposing defaultLoginPage, $message - $isProcessing")
    Surface(
        modifier = Modifier
//            .wrapContentSize(Alignment.Center)
//            .padding(dimensionResource(R.dimen.form_page_padding))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // .padding(horizontal = dimensionResource(R.dimen.card_padding))
                .padding(dimensionResource(R.dimen.form_page_padding))
                .wrapContentWidth(Alignment.Start)
        ) {

            TitleDescColumnBloc(title, desc)

            Content()

            if (Str.notEmpty(message)) {
                val textColor = if (isErrorMsg)
                    MaterialTheme.colorScheme.error
                else
                    Color.Unspecified
                Text(
                    text = message!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = textColor,
                )
            }

            if (isProcessing) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
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

@Preview(name = "SkipVerify Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "SkipVerify Dark"
)
@Composable
private fun SkipVerifyPreview() {
    CellsTheme {
        SkipVerify(
            isProcessing = true,
            fqdn = "https://files.example.com",
            message = "pinging server",
            errMsg = null,
            { },
            { },
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

@Preview(name = "ProcessAuth Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "ProcessAuth Dark"
)
@Composable
private fun ProcessAuthPreview() {
    CellsTheme {
        ProcessAuth(
            isProcessing = true,
            message = "Gettting credentials...",
        )
    }
}
