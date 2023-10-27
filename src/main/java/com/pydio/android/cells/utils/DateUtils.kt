package com.pydio.android.cells.utils

import com.pydio.android.cells.AppNames
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun asSinceString(timestamp: Long): String {

    val tMin = 60L
    val tHour = 60L * tMin
    val tDay: Long = 24L * tHour

    val agoLong = currentTimestamp() - timestamp;

    return when {
        agoLong < tMin -> "since a few seconds"
        agoLong < 2 * tMin -> "since a minute"
        agoLong < tHour -> String.format("since %s minutes", agoLong / tMin)
        agoLong < tDay * 2 -> String.format("since %s hours", agoLong / tHour)
        agoLong < tDay * 7 -> String.format("since %s days", agoLong / tDay)
        else -> getTimestampAsString(timestamp)
    }
}

fun fromFreqToMinuteInterval(freq: String?): Long {
    return when (freq) {
        AppNames.SYNC_FREQ_QUARTER -> 15 // this is the minimum supported by the work manager
        AppNames.SYNC_FREQ_HOUR -> 60
        AppNames.SYNC_FREQ_DAY -> 60 * 24
        else -> 60 * 24 * 7 // default is every week
    }
}

fun asAgoString(timestamp: Long): String {

    val tMin = 60L
    val tHour = 60L * tMin
    val tDay: Long = 24L * tHour

    val agoLong = currentTimestamp() - timestamp;

    return when {
        agoLong < tMin -> "a few seconds ago"
        agoLong < 2 * tMin -> "a minute ago"
        agoLong < tHour -> String.format("%s minutes ago", agoLong / tMin)
        agoLong < tDay * 2 -> String.format("%s hours ago", agoLong / tHour)
        agoLong < tDay * 7 -> String.format("%s days ago", agoLong / tDay)
        else -> getTimestampAsString(timestamp)
    }
}

fun Date.asFormattedString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}

/**
 * Centralise generation of timestamp to ease potential later refactoring.
 * We currently rely on number of seconds since 1970.
 */
fun currentTimestamp(): Long {
    return System.currentTimeMillis() / 1000L
}

fun currentTimestampAsString(): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(System.currentTimeMillis())
}

fun timestampForLogMessage(): String {
    val formatter = SimpleDateFormat("dd/MM/yy' at 'HH:mm", Locale.US)
    return formatter.format(System.currentTimeMillis())
}

fun timestampToString(timestamp: Long, pattern: String): String {
    val formatter = SimpleDateFormat(pattern, Locale.US)
    return formatter.format(timestamp * 1000)
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getTimestampAsString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)
}

// Shorter name
fun getTsAsString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)
}

fun getTimestampAsENString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(timestamp)
}
