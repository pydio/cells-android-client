package com.pydio.android.cells.ui.login.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.login.LoginViewModelNew
import com.pydio.android.cells.ui.login.nav.StateViewModel
import com.pydio.android.cells.ui.theme.CellsTheme

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProcessAuth(
    stateVM: StateViewModel,
    loginVM: LoginViewModelNew,
    navigateTo: (String?) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val message = loginVM.message.collectAsState()

    // FIXME trigger Nav when auth has been done

    ProcessAuth(
        isProcessing = true,
        message = message.value
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
    CellsTheme {
        ProcessAuth(
            isProcessing = true,
            message = "Getting credentials...",
        )
    }
}
