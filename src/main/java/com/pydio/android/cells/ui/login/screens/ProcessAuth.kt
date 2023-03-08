package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.TitleDescColumnBloc
import com.pydio.android.cells.ui.login.LoginHelper
import com.pydio.android.cells.ui.login.models.LoginVM
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "ProcessAuth"

@Composable
fun ProcessAuth(
    stateID: StateID,
    skipVerify: Boolean,
    loginVM: LoginVM,
    helper: LoginHelper,
) {
    val message = loginVM.message.collectAsState()
    val errMsg = loginVM.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(key1 = stateID, helper.startingState) {
        Log.e(logTag, "About to launch process Auth for ${helper.startingState?.route}")
        helper.processAuth(context, stateID, skipVerify)
    }

    ProcessAuth(
        isProcessing = Str.empty(errMsg.value),
        message = message.value,
        errMsg = errMsg.value,
        cancel = helper::cancel
    )
}

@Composable
fun ProcessAuth(
    isProcessing: Boolean,
    message: String?,
    errMsg: String?,
    cancel: () -> Unit,
) {

    val title = stringResource(R.string.oauth_code_flow_title)
    val desc = stringResource(R.string.oauth_code_flow_desc)

    Surface(modifier = Modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // .padding(horizontal = dimensionResource(R.dimen.card_padding))
                .padding(dimensionResource(R.dimen.form_page_padding))
                .wrapContentWidth(Alignment.Start)
        ) {

            TitleDescColumnBloc(title, desc)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .size(240.dp)
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
            if (isProcessing) {
                CircularProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .alpha(.8f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (Str.notEmpty(errMsg)) {
                Text(
                    text = errMsg!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (Str.notEmpty(message)) {
                Text(
                    text = message!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally),
                )
            }

            TextButton(
                onClick = cancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp
                    )
                    .wrapContentWidth(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.button_cancel).uppercase(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(
                modifier = Modifier
                    .weight(0.2f)
                    .padding(bottom = dimensionResource(R.dimen.margin_header))
            )
        }
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
            message = "Getting credentials...",
            errMsg = null,
            cancel = {}
        )
    }
}
