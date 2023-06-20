package com.pydio.android.cells.utils

import android.os.Build
import android.util.Log
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListType
import com.pydio.cells.api.SDKException
import java.io.File

//fun Fragment.hideKeyboard() {
//    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
//}

/* VARIOUS */

fun childFile(parPath: String, filename: String): File {
    return File(parPath + File.separator + filename)
}

/* CURRENT SYSTEM INFORMATION */

fun getOSCurrentVersion(): String {
    val release = java.lang.Double.parseDouble(
        java.lang.String(Build.VERSION.RELEASE).replaceAll("(\\d+[.]\\d+)(.*)", "$1")
    )
    var codeName = "Unsupported" // Older as Jelly Bean
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

fun logException(caller: String?, msg: String, e: Exception) {
    Log.e(caller, "$msg ${if (e is SDKException) "(Code #${e.code} )" else ""}")
    e.printStackTrace()
}

/* MANAGE PREFERENCES */

// fun parseOrder(encoded: String, type: ListType = ListType.DEFAULT): Pair<String, String> {
//  Returns the default value for each list type if the passed encoded value is empty or not valid
fun parseOrder(encoded: String?, type: ListType): Pair<String, String> {
    var tokens = encoded?.split("||") ?: "".split("||")
    if (tokens.size != 2) {
        Log.w("parseOrder", "could not parse encoded order [$encoded]")
        val newDefault = when (type) {
            ListType.JOB -> AppNames.JOB_DEFAULT_ENCODED_ORDER
            ListType.TRANSFER -> AppNames.TRANSFER_DEFAULT_ENCODED_ORDER
            ListType.DEFAULT -> AppNames.DEFAULT_SORT_ENCODED
        }
        tokens = newDefault.split("||")
    }
    return Pair(tokens[0], tokens[1])
}

//fun decodeSortById(sortId: String): Pair<String, String> {
//    val prefix = sortId.substring(0, sortId.lastIndexOf("_"))
//    val suffix = sortId.substring(sortId.lastIndexOf("_") + 1)
//    return prefix to if (suffix == "desc") "DESC" else "ASC"
//}
