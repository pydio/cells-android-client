package com.pydio.android.cells.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.pydio.cells.api.SDKException
import com.pydio.cells.utils.Log
import com.pydio.android.cells.AppNames
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

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
    val release = java.lang.Double.parseDouble(java.lang.String(Build.VERSION.RELEASE).replaceAll("(\\d+[.]\\d+)(.*)", "$1"))
    var codeName = "Unsupported"//below Jelly Bean
    if (release >= 4.1 && release < 4.4)  codeName = "Jelly Bean"
    else if (release < 5)   codeName = "Kit Kat"
    else if (release < 6)   codeName = "Lollipop"
    else if (release < 7)   codeName = "Marshmallow"
    else if (release < 8)   codeName = "Nougat"
    else if (release < 9)   codeName = "Oreo"
    else if (release < 10)  codeName = "Pie"
    else if (release >= 10) codeName = "Android "+(release.toInt())//since API 29 no more candy code names
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
    if (tokens.size != 2 ){
        Log.e("parseOrder", "could not parse encoded order $encoded")
        return Pair(AppNames.DEFAULT_SORT_BY, AppNames.DEFAULT_SORT_BY_DIR)
    }
    return Pair(tokens[0], tokens[1])
}

/* HELPERS TO MANAGE DATES */

fun Date.asFormattedString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

/**
 * Centralise generation of timestamp to ease potential later refactoring.
 * We currently rely on number of seconds since 1970.
 */
fun currentTimestamp() : Long {
    return System.currentTimeMillis() / 1000L
}

fun currentTimestampAsString() : String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(System.currentTimeMillis())
}

fun timestampForLogMessage() : String {
    val formatter = SimpleDateFormat("dd/MM/yy' at 'HH:mm", Locale.US)
    return formatter.format(System.currentTimeMillis())
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getTimestampAsString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)
}

fun getTimestampAsENString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(timestamp)
}
