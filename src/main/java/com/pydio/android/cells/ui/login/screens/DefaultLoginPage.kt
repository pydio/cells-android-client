package com.pydio.android.cells.ui.login.screens

import android.util.Log
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
import com.pydio.android.cells.ui.box.common.TitleDescColumnBloc
import com.pydio.cells.utils.Str

private const val logTag = "DefaultLoginPage.kt"

@Composable
fun DefaultLoginPage(
    isProcessing: Boolean,
    title: String,
    desc: String?,
    message: String?,
    isErrorMsg: Boolean = false,
    Content: @Composable () -> Unit,
) {
    Surface(modifier = Modifier) {
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
                Log.e(logTag, "... Recomposing defaultLoginPage for msg: $message")

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
                Log.e(logTag, "... Recomposing defaultLoginPage with processing state")
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
