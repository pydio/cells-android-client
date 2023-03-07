package com.pydio.android.cells.ui.core

import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.navigation.NavBackStackEntry
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.share.ShareDestination
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str

private const val logTag = "core.utils"

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
        ?.let {
            // Log.e(logTag, " ... Retrieving query context from backstack entry, found: $it")
            it
        }
        ?: run {
            Log.e(logTag, " ... No query context found in backstack entry, for key $key")
            "none"
        }
}


fun lazySkipVerify(
    navBackStackEntry: NavBackStackEntry?,
    key: String = LoginDestinations.ProcessAuth.getSkipVerifyKey(),
): Boolean {
    return navBackStackEntry?.arguments?.getBoolean(key) ?: false
}

fun lazyUID(
    navBackStackEntry: NavBackStackEntry?,
    key: String = ShareDestination.UploadInProgress.getUidKey(),
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
