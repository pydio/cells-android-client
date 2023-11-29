package com.pydio.android.cells.utils

import kotlin.math.min

class BackOffTicker {
    private val backoffDuration = longArrayOf(
        2, 3, 5, 10, 15, 20,
        30, 30, 30, 30, 30, 30,
        40, 60, 60, 120, 120, 300,
        600, 600, 900, 900, 900, 1800, 3600
    )
    private var currentBackoffIndex = 0

    /** Returns the next delay duration, in seconds */
    fun getNextDelay(): Long {
        synchronized(this) {
            val nextDelay = backoffDuration[currentBackoffIndex]
            currentBackoffIndex = min(currentBackoffIndex + 1, backoffDuration.size - 1)
            return nextDelay
        }
    }

    fun getCurrentDelay(): Long {
        synchronized(this) {
            return backoffDuration[currentBackoffIndex]
        }
    }

    fun getCurrentIndex(): Int {
        synchronized(this) {
            return currentBackoffIndex
        }
    }

    fun resetIndex() {
        synchronized(this) {
            currentBackoffIndex = 0
        }
    }
}
