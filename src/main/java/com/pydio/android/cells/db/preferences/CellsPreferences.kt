package com.pydio.android.cells.db.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.ui.core.ListLayout

const val CELLS_PREFERENCES_NAME = "cells_preferences"

data class CellsPreferences(
    val versionCode: Int,
    val showDebugTools: Boolean,
    val disablePoll: Boolean,
    val list: ListPreferences,
    val meteredNetwork: MeteredNetworkPreferences,
    val sync: SyncPreferences
)

data class ListPreferences(
    val layout: ListLayout,
    val order: String,
    val transferOrder: String,
    val transferFilter: String,
    val jobOrder: String,
    val jobFilter: String,
)

data class MeteredNetworkPreferences(
    val applyLimits: Boolean,
    val showWarning: Boolean,
    val dlThumbs: Boolean,
    val askBeforeDL: Boolean,
    val sizeThreshold: Long,
)

data class SyncPreferences(
    val frequency: String,
    val onNetworkType: String,
    val onCharging: Boolean,
    val onBatteryNotLow: Boolean,
    val onIdle: Boolean,
)

fun defaultCellsPreferences(): CellsPreferences {
    val currVersion = -1
    val showDebug = false
    val disablePoll = false

    // List order, layout and filters
    val listPref = ListPreferences(
        order = AppNames.DEFAULT_SORT_ENCODED,
        layout = ListLayout.LIST,
        transferOrder = AppNames.TRANSFER_DEFAULT_ENCODED_ORDER,
        transferFilter = JobStatus.NO_FILTER.id,
        jobOrder = AppNames.JOB_DEFAULT_ENCODED_ORDER,
        jobFilter = JobStatus.NO_FILTER.id,
    )
    // Metered network limitations
    val meteredPref = MeteredNetworkPreferences(
        applyLimits = true,
        showWarning = true,
        dlThumbs = false,
        askBeforeDL = true,
        sizeThreshold = 10,
    )
    // Offline settings
    val syncPref = SyncPreferences(
        frequency = AppNames.SYNC_FREQ_WEEK,
        onNetworkType = AppNames.NETWORK_TYPE_UNMETERED,
        onCharging = true,
        onBatteryNotLow = true,
        onIdle = true
    )
    return CellsPreferences(currVersion, showDebug, disablePoll, listPref, meteredPref, syncPref)
}

// Migration from legacy SharedPreference system

const val LEGACY_PREFERENCES_KEY = "com.pydio.android.Client_preferences"

val legacyMigrations: (Context) -> List<DataMigration<Preferences>> = { context ->
    // Since we're migrating from SharedPreferences, add a migration based on the SharedPreferences name
    listOf(SharedPreferencesMigration(context, LEGACY_PREFERENCES_KEY))
}
