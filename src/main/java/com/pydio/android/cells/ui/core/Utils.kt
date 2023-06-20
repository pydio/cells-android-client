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
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.net.URLDecoder
import java.net.URLEncoder

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

fun encodeStateForRoute(stateID: StateID): String {
    return URLEncoder.encode(stateID.id, "UTF-8")
}

fun lazyStateID(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.STATE_ID,
): StateID {
    return navBackStackEntry?.arguments?.getString(key)
        ?.let {
            Log.e(logTag, " ... Retrieving stateID from backstack entry, found: $it")
            tweakedFromId(it)
        }
        ?: run {
            // Log.w(logTag, " ... No stateID found in backstack entry, for key $key")
            StateID.NONE
        }
}


private fun tweakedFromId(stateId: String?): StateID? {

    if (stateId.isNullOrEmpty()) {
        return null
    }

    var username: String? = null;
    var host: String? = null;
    var path: String? = null;

    return try {
        val parts = stateId.split("@".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        when (parts.size) {
            1 -> host = URLDecoder.decode(parts[0], "UTF-8")
            2 -> {
                username = URLDecoder.decode(parts[0], "UTF-8")
                host = URLDecoder.decode(parts[1], "UTF-8")
            }

            3 -> {
                username = URLDecoder.decode(parts[0], "UTF-8")
                host = URLDecoder.decode(parts[1], "UTF-8")
                path = URLDecoder.decode(parts[2], "UTF-8")
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    Log.e(logTag, "Had to tweak $stateId")
                    username = "$username@$host"
                    host = path
                    path = null
                }
            }

            4 -> {
                Log.e(logTag, "Had to tweak $stateId")
                username = URLDecoder.decode(parts[0], "UTF-8")
                host = URLDecoder.decode(parts[1], "UTF-8")
                path = URLDecoder.decode(parts[2], "UTF-8")

                if (path.startsWith("http://") || path.startsWith("https://"))
                    username = "$username@$host"
                host = path
                path = URLDecoder.decode(parts[3], "UTF-8")
            }

            else -> {
                Log.e(logTag, "Could not create State from ID: $stateId")
                return null
            }
        }
        StateID(username, host, path)

    } catch (iae: IllegalArgumentException) {
        Log.e(logTag, "Could not decode [$stateId] - cause:$iae")
        iae.printStackTrace();
        return null;
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
