package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.ui.core.screens.AuthScreen
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun LaunchOAuthFlow(
    stateID: StateID,
    skipVerify: Boolean,
    loginContext: String,
    loginVM: LoginVM,
    helper: LoginHelper,
) {
    val logTag = "LaunchAuthProcessing"

    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(key1 = stateID) {
        Log.i(logTag, "... Launch auth process for $stateID")
        helper.launchAuth(context, stateID, skipVerify, loginContext)
    }

    AuthScreen(
        isProcessing = errMsg.value.isNullOrEmpty(),
        message = message.value,
        errMsg = errMsg.value,
        cancel = helper::cancel
    )
}

@Preview(name = "ProcessAuth Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "ProcessAuth Dark"
)
@Composable
private fun ProcessAuthPreview() {
    UseCellsTheme {
        AuthScreen(
            isProcessing = true,
            message = "Getting credentials...",
            errMsg = null,
            cancel = {}
        )
    }
}
