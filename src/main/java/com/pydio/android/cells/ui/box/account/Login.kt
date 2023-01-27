package com.pydio.android.cells.ui.box.account

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.DefaultTitleText
import com.pydio.android.cells.ui.models.LoginVM
import com.pydio.android.cells.ui.theme.CellsTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskServerUrl(
    loginVM: LoginVM,
) {

    val isProcessing = loginVM.isProcessing.collectAsState()
    val urlString = rememberSaveable { mutableStateOf("https://") }
    val updateUrl: (String) -> Unit = { urlString.value = it }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText(stringResource(R.string.ask_url_title))
        Surface(
            tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_medium),
                    vertical = dimensionResource(R.dimen.text_padding_small)
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.text_padding_small),
                        vertical = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.ask_url_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )

                TextField(
                    value = urlString.value,
                    onValueChange = { updateUrl(it) },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://www.example.com") },
//                    supportingText = {
//                        Text("https://www.example.com")
//                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // TODO add sanity checks
                            scope.launch {
                                loginVM.pingAddress(urlString.value)
                            }
                        }
                    ),
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small)),
                )

                if (isProcessing.value) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensionResource(R.dimen.margin_medium))
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            loginVM.pingAddress(urlString.value)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = dimensionResource(R.dimen.margin))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.button_next)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P8Credentials(
    loginVM: LoginVM,
) {
    val isProcessing = loginVM.isProcessing.collectAsState()
    val loginString = rememberSaveable { mutableStateOf("") }
    val updateLogin: (String) -> Unit = { loginString.value = it }
    val pwdString = rememberSaveable { mutableStateOf("") }
    val updatePwd: (String) -> Unit = { pwdString.value = it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText(stringResource(R.string.p8_credentials_title))
        Surface(
            tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_medium),
                    vertical = dimensionResource(R.dimen.text_padding_small)
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.text_padding_small),
                        vertical = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.p8_credentials_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )

                TextField(
                    value = loginString.value,
                    onValueChange = { updateLogin(it) },
                    supportingText = {
                        Text("Login")
                    },
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small)),
                )

                TextField(
                    value = pwdString.value,
                    onValueChange = { updatePwd(it) },
                    supportingText = {
                        Text("Password")
                    },
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small)),
                )

                if (isProcessing.value) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensionResource(R.dimen.margin_medium))
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
                TextButton(
                    onClick = {
                        // TODO add validation
                        loginVM.logToP8(loginString.value, pwdString.value, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = dimensionResource(R.dimen.margin))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.button_next)) }
            }
        }
    }
}

@Composable
fun ProcessAuth(
    loginVM: LoginVM,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText("Processing...")
        LinearProgressIndicator(
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.margin_medium))
                .wrapContentWidth(Alignment.CenterHorizontally)
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
//         ProcessAuth()
    }
}

