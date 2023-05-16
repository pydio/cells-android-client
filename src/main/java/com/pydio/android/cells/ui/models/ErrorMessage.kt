package com.pydio.android.cells.ui.models

import android.content.Context
import androidx.annotation.StringRes
import com.pydio.android.cells.R

const val unknownError = R.string.generic_error_message
const val undefined = -1

data class ErrorMessage(
    val defaultMessage: String?,
    @StringRes val id: Int,
    val formatArgs: List<Any>,
)

fun toErrorMessage(context: Context, msg: ErrorMessage): String {
    return msg.defaultMessage ?: context.getString(
        msg.id,
        *msg.formatArgs.map { it }.toTypedArray()
    )
}

fun fromException(e: Exception): ErrorMessage {
    return ErrorMessage(e.message, unknownError, listOf())
}

fun fromMessage(msg: String): ErrorMessage {
    return ErrorMessage(msg, undefined, listOf())
}