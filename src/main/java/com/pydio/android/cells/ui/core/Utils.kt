package com.pydio.android.cells.ui.core

import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "core.utils"


@Composable
fun getMessageFromLocalModifStatus(status: String): String? {
    return when (status) {
        AppNames.LOCAL_MODIF_DELETE -> stringResource(R.string.in_progress_deleting)
        AppNames.LOCAL_MODIF_RENAME -> stringResource(R.string.in_progress_renaming)
        AppNames.LOCAL_MODIF_MOVE -> stringResource(R.string.in_progress_moving)
        AppNames.LOCAL_MODIF_RESTORE -> stringResource(R.string.in_progress_restoring)
        else -> null
    }
}

fun getFloatResource(context: Context, @DimenRes id: Int): Float {
    val outValue = TypedValue()
    context.resources.getValue(id, outValue, true)
    return outValue.float
}

fun lazyStateID(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.STATE_ID,
): StateID {
    return navBackStackEntry?.arguments?.getString(key)
        ?.let {
            // Log.e(logTag, " ... Retrieving stateID from backstack entry, found: $it")
            StateID.fromId(it)
        }
        ?: run {
            // Log.w(logTag, " ... No stateID found in backstack entry, for key $key")
            StateID.NONE
        }
}

fun lazyQueryContext(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.QUERY_CONTEXT,
): String {
    return navBackStackEntry?.arguments?.getString(key)
        ?: run {
            Log.e(logTag, " ... No query context found in backstack entry, for key $key")
            "none"
        }
}


fun lazySkipVerify(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.SKIP_VERIFY,
): Boolean {
    val skipStr = navBackStackEntry?.arguments?.getString(key)
    return skipStr?.let { it == "true" } ?: false
}

fun lazyUID(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.UID,
): Long {
    val stringValue = navBackStackEntry?.arguments?.getString(key)
    if (Str.notEmpty(stringValue)) {
        try {
            return stringValue!!.toLong()
        } catch (nfe: NumberFormatException) {
            Log.e(logTag, "Un-valid jobID format: [$stringValue]")
        }
    }
    return 0L
}
