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
import com.pydio.android.cells.services.AuthService
import com.pydio.cells.transport.StateID
import java.net.URLDecoder
import java.net.URLEncoder

private const val logTag = "core.utils"

@Composable
fun getMessageFromLocalModifStatus(status: String): String? {
    return when (status) {
        AppNames.LOCAL_MODIF_UPDATE -> stringResource(R.string.in_progress_updating)
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
    verbose: Boolean = true
): StateID {
    return navBackStackEntry?.arguments?.getString(key)?.let {
        // Log.e(logTag, " ... Retrieving stateID from backstack entry, found: $it")
        if (it.isEmpty()) {
            return StateID.NONE
        }
        return StateID.fromId(it)
    } ?: run {
        if (verbose) {
            Log.w(logTag, " ... No stateID found in backstack entry with key $key")
        }
        StateID.NONE
    }
}

fun encodeStateSetForRoute(stateIDs: Set<StateID>): String {
    val reduced = stateIDs.map { URLEncoder.encode(it.id, "UTF-8") }.reduce { acc, curr ->
        if (acc.isEmpty()) {
            curr
        } else {
            "${acc}&$curr"
        }
    }
    return URLEncoder.encode(reduced, "UTF-8")
}


fun lazyStateIDs(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.STATE_IDS,
): Set<StateID> {
    return navBackStackEntry?.arguments?.getString(key)?.let {
        // Log.e(logTag, " ... Retrieving stateID from backstack entry, found: $it")
        if (it.isEmpty()) {
            setOf(StateID.NONE)
        } else {
            val reduced = URLDecoder.decode(it, "UTF-8")
            val ids = mutableSetOf<StateID>()
            reduced.split("&").forEach { currEncoded ->
                if (currEncoded.isNotEmpty()) {
                    val currID = StateID.fromId(URLDecoder.decode(currEncoded, "UTF-8"))
                    if (currID != StateID.NONE) {
                        ids.add(currID)
                    }
                }
            }
            if (ids.isEmpty()) { // TODO double check if it is really necessary
                ids.add(StateID.NONE)
            }
            ids
        }
    } ?: run {
        Log.w(logTag, " ... No stateID found in backstack entry with key $key")
        setOf(StateID.NONE)
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

fun lazyLoginContext(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.LOGIN_CONTEXT,
): String {
    val skipStr = navBackStackEntry?.arguments?.getString(key)
    return skipStr ?: AuthService.LOGIN_CONTEXT_CREATE
}

fun lazyUID(
    navBackStackEntry: NavBackStackEntry?,
    key: String = AppKeys.UID,
): Long {
    val stringValue = navBackStackEntry?.arguments?.getString(key)
    if (!stringValue.isNullOrEmpty()) {
        try {
            return stringValue.toLong()
        } catch (nfe: NumberFormatException) {
            Log.e(logTag, "Un-valid jobID format: [$stringValue]")
        }
    }
    return 0L
}

fun dumpNavigationStack(
    logTag: String,
    context: String,
    bseList: List<NavBackStackEntry>,
    nextRoute: String?
) {
    val builder = StringBuilder("... Dumping backstack from $context")
    nextRoute?.let {
        builder.append(" B4 nav to: $nextRoute")
    }
    builder.append("\n")
    var i = 1
    for (bse in bseList) {
        builder.append("\t #$i: ${bse.destination.route} - ${lazyStateID(bse, verbose = false)}\n")
        i++
    }
    Log.i(logTag, builder.toString())
}
