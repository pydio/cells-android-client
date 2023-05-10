package com.pydio.android.cells.ui.models

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class ErrorMessage(
    val defaultMessage: String?,
    @StringRes val id: Int,
    val formatArgs: List<Any>,
)

@Composable
fun toErrorMessage(msg: ErrorMessage): String {
    return msg.defaultMessage ?: stringResource(
        id = msg.id,
        *msg.formatArgs.map { it }.toTypedArray()
    )
}
