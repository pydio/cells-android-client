package com.pydio.android.cells.utils

import com.pydio.android.cells.AppNames
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class DateUtils {
}

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
        AppNames.OFFLINE_FREQ_QUARTER -> 15 // this is the minimum supported by the work manager
        AppNames.OFFLINE_FREQ_HOUR -> 60
        AppNames.OFFLINE_FREQ_DAY -> 60 * 24
        else -> 60 * 24 * 7
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


//<string name="last_sync_few_secs_ago">Last checked a few seconds ago</string>
//<plurals name="last_sync_x_minutes_ago">
//<item quantity="other">Last checked %d min ago</item>
//</plurals>
//<plurals name="last_sync_x_hours_ago">
//<item quantity="one">Last checked %d hour ago</item>
//<item quantity="other">Last checked %d hours ago</item>
//</plurals>
//<plurals name="last_sync_x_days_ago">
//<item quantity="one">Last checked %d day ago</item>
//<item quantity="other">Last checked %d days ago</item>
//</plurals>
//<plurals name="last_sync_x_months_ago">
//<item quantity="one">Last checked %d month ago</item>
//<item quantity="other">Last checked %d months ago</item>
//</plurals>

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

fun getTimestampAsENString(timestamp: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(timestamp)
}