package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.login.LoginViewModelNew
import com.pydio.android.cells.ui.login.nav.StateViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.utils.Str
import kotlinx.coroutines.launch

private const val logTag = "SkipVerify"

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
fun SkipVerify(
    stateVM: StateViewModel,
    loginVM: LoginViewModelNew,
    navigateTo: (String?) -> Unit,
) {

    Log.e(logTag, "Nav to Login Step")

    val scope = rememberCoroutineScope()
    val isProcessing = loginVM.isProcessing.collectAsState()
    val currAddress = loginVM.serverAddress.collectAsState()
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val doPing: (String) -> Unit = { url ->
        // TODO add sanity checks
        scope.launch {
            loginVM.pingAddress()
        }
    }

    SkipVerify(
        isProcessing.value,
        currAddress.value,
        message.value,
        errMsg.value,
        goBack = { navigateTo(null) },
        accept = { scope.launch { loginVM.confirmSkipVerifyAndPing() } },
    )
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