package com.pydio.android.cells.ui.login.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.TitleDescColumnBloc

// private const val logTag = "DefaultLoginPage"

@Composable
fun DefaultLoginPage(
    isProcessing: Boolean,
    title: String,
    desc: String?,
    message: String?,
    isErrorMsg: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(modifier = Modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.form_page_padding))
                .wrapContentWidth(Alignment.Start)
        ) {

            TitleDescColumnBloc(title, desc)

            content()

            if (!message.isNullOrEmpty()) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    color = if (isErrorMsg)
                        MaterialTheme.colorScheme.error
                    else
                        Color.Unspecified,
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
