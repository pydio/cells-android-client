package com.pydio.android.cells.ui.core

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.navigation.NavBackStackEntry
import com.pydio.android.cells.ui.browse.BrowseDestinations
import com.pydio.cells.api.Transport
import com.pydio.cells.transport.StateID

fun getFloatResource(context: Context, @DimenRes id: Int): Float {
    val outValue = TypedValue()
    context.resources.getValue(id, outValue, true)
    return outValue.float
}


fun lazyID(navBackStackEntry: NavBackStackEntry): StateID {
    return navBackStackEntry.arguments?.getString(BrowseDestinations.Open.getPathKey())
        ?.let { StateID.fromId(it) }
        ?: Transport.UNDEFINED_STATE_ID
}
