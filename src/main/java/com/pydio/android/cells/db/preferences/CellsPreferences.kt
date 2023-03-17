package com.pydio.android.cells.db.preferences

import android.content.Context
import android.content.res.Resources
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.ListLayout

const val CELLS_PREFERENCES_NAME = "cells_preferences"

data class CellsPreferences(
    val versionCode: Int,
    val showDebugTools: Boolean,
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

fun ListPreferences.orderLabel(resource: Resources): String {
    val labels = resource.getStringArray(R.array.order_by_labels)
    val keys = resource.getStringArray(R.array.order_by_values)
    keys.forEachIndexed { index, key ->
        if (key == this.order) {
            return labels[index]
        }
    }
    return "-"
}

data class MeteredNetworkPreferences(
    val applyLimits: Boolean,
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

fun SyncPreferences.frequencyLabel(resource: Resources): String {
    val labels = resource.getStringArray(R.array.offline_frequency_labels)
    val keys = resource.getStringArray(R.array.offline_frequency_values)
    keys.forEachIndexed { index, key ->
        if (key == this.frequency) {
            return labels[index]
        }
    }
    return "-"
}

fun SyncPreferences.onNetworkTypeLabel(resource: Resources): String {
    val labels = resource.getStringArray(R.array.network_type_labels)
    val keys = resource.getStringArray(R.array.network_type_values)
    keys.forEachIndexed { index, key ->
        if (key == this.onNetworkType) {
            return labels[index]
        }
    }
    return "-"
}

fun defaultCellsPreferences(): CellsPreferences {
    val currVersion = -1
    val showDebug = false

    // List order, layout and filters
    val listPref = ListPreferences(
        order = AppNames.DEFAULT_SORT_ENCODED,
        layout = ListLayout.LIST,
        transferOrder = AppNames.DEFAULT_SORT_ENCODED,
        transferFilter = AppNames.JOB_STATUS_NO_FILTER,
        jobOrder = AppNames.JOB_SORT_BY_DEFAULT, //AppNames.DEFAULT_SORT_ENCODED,
        jobFilter = AppNames.JOB_STATUS_NO_FILTER,
    )
    // Metered network limitations
    val meteredPref = MeteredNetworkPreferences(
        applyLimits = true,
        dlThumbs = false,
        askBeforeDL = true,
        sizeThreshold = -1,
    )
    // Offline settings
    val syncPref = SyncPreferences(
        frequency = AppNames.SYNC_FREQ_WEEK,
        onNetworkType = AppNames.NETWORK_TYPE_UNMETERED,
        onCharging = true,
        onBatteryNotLow = true,
        onIdle = true
    )
    return CellsPreferences(currVersion, showDebug, listPref, meteredPref, syncPref)
}

// Migration from legacy SharedPreference system

const val LEGACY_PREFERENCES_KEY = "com.pydio.android.Client_preferences"

val legacyMigrations: (Context) -> List<DataMigration<Preferences>> = { context ->
    // Since we're migrating from SharedPreferences, add a migration based on the SharedPreferences name
    listOf(SharedPreferencesMigration(context, LEGACY_PREFERENCES_KEY))
}
