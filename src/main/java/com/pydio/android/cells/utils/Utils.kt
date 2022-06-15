package com.pydio.android.cells.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.pydio.android.cells.AppNames
import com.pydio.cells.api.SDKException
import com.pydio.cells.utils.Log
import java.io.File

/* UI */

fun Fragment.hideKeyboard() {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
}

/* VARIOUS */

fun childFile(parPath: String, filename: String): File {
    return File(parPath + File.separator + filename)
}

/* CURRENT SYSTEM INFORMATION */

fun getOSCurrentVersion(): String {
    val release = java.lang.Double.parseDouble(
        java.lang.String(Build.VERSION.RELEASE).replaceAll("(\\d+[.]\\d+)(.*)", "$1")
    )
    var codeName = "Unsupported"//below Jelly Bean
    if (release >= 4.1 && release < 4.4) codeName = "Jelly Bean"
    else if (release < 5) codeName = "Kit Kat"
    else if (release < 6) codeName = "Lollipop"
    else if (release < 7) codeName = "Marshmallow"
    else if (release < 8) codeName = "Nougat"
    else if (release < 9) codeName = "Oreo"
    else if (release < 10) codeName = "Pie"
    else if (release >= 10) codeName =
        "Android " + (release.toInt())//since API 29 no more candy code names
    return codeName + " v" + release + ", API Level: " + Build.VERSION.SDK_INT
}

@Suppress("DEPRECATION")
fun hasUnMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return connMgr.isDefaultNetworkActive && !connMgr.isActiveNetworkMetered
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_WIFI) {
                    return true
                }
            }
        }
        return false
    }
}

@Suppress("DEPRECATION")
fun hasMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        return connMgr.isDefaultNetworkActive && connMgr.isActiveNetworkMetered
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_MOBILE) {
                    return true
                }
            }
        }
        return false
    }
}

fun hasAtLeastMeteredNetwork(context: Context): Boolean {

    val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        connMgr.activeNetwork?.let {
            // Log.i("ConnectionCheck", "Active network: ${it.networkHandle}")
            return true
        }
        return false
    } else {
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkInfo(network)?.let {
                if (it.type == ConnectivityManager.TYPE_MOBILE || it.type == ConnectivityManager.TYPE_WIFI) {
                    return true
                }
            }
        }
        return false
    }
}

fun logException(caller: String?, msg: String, e: Exception) {
    Log.e(caller, "$msg ${if (e is SDKException) "(Code #${e.code} )" else ""}")
    e.printStackTrace()
}

/* MANAGE PREFERENCES */

fun parseOrder(encoded: String): Pair<String, String> {
    val tokens = encoded.split("||")
    if (tokens.size != 2) {
        Log.e("parseOrder", "could not parse encoded order $encoded")
        return Pair(AppNames.DEFAULT_SORT_BY, AppNames.DEFAULT_SORT_BY_DIR)
    }
    return Pair(tokens[0], tokens[1])
}

/* HELPERS TO MANAGE DATES */

