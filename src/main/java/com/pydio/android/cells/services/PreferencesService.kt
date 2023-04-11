package com.pydio.android.cells.services

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pydio.android.cells.ListType
import com.pydio.android.cells.db.preferences.CellsPreferences
import com.pydio.android.cells.db.preferences.ListPreferences
import com.pydio.android.cells.db.preferences.MeteredNetworkPreferences
import com.pydio.android.cells.db.preferences.SyncPreferences
import com.pydio.android.cells.db.preferences.defaultCellsPreferences
import com.pydio.android.cells.ui.core.ListLayout
import com.pydio.android.cells.utils.parseOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// See https://developer.android.com/codelabs/android-preferences-datastore for some more info
object PreferencesKeys {

    // To manage app migrations
    val INSTALLED_VERSION_CODE = intPreferencesKey("installed_version_code")

    // Show technical pages
    val SHOW_DEBUG_TOOLS = booleanPreferencesKey("show_debug_tools")
    val DISABLE_POLL = booleanPreferencesKey("disable_poll")

    // List order, layout and filters
    val DEFAULT_LIST_ORDER = stringPreferencesKey("current_recycler_order")
    val DEFAULT_LIST_LAYOUT = stringPreferencesKey("current_recycler_layout")
    val TRANSFER_SORT_BY = stringPreferencesKey("transfer_sort_by")
    val TRANSFER_FILTER_BY_STATUS = stringPreferencesKey("transfer_filter_by_status")
    val JOB_SORT_BY = stringPreferencesKey("job_sort_by")
    val JOB_FILTER_BY_STATUS = stringPreferencesKey("job_filter_by_status")

    // Metered network limitations
    val APPLY_METERED_LIMITATION = booleanPreferencesKey("apply_metered_limitations")
    val METERED_DL_THUMBS = booleanPreferencesKey("on_metered_dl_thumbs")
    val METERED_ASK_B4_DL_FILES = booleanPreferencesKey("on_metered_ask_before_dl_files")
    val METERED_ASK_B4_DL_FILES_SIZE =
        longPreferencesKey("on_metered_ask_before_dl_files_greater_than")

    // Offline settings
    val SYNC_FREQ = stringPreferencesKey("sync_frequency")
    val SYNC_CONST_NETWORK_TYPE = stringPreferencesKey("sync_network_type")
    val SYNC_CONST_ON_CHARGING = booleanPreferencesKey("sync_on_charging")
    val SYNC_CONST_ON_BATT_NOT_LOW = booleanPreferencesKey("sync_on_batt_not_low")
    val SYNC_CONST_ON_IDLE = booleanPreferencesKey("sync_on_idle")
}

class PreferencesService(private val dataStore: DataStore<Preferences>) {

    private val logTag = "PreferencesService"
    private val noPref = defaultCellsPreferences()

    val cellsPreferencesFlow: Flow<CellsPreferences> = dataStore.data
        .catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val currPrefs = mapCellsPreferences(preferences)
            currPrefs
        }

    suspend fun fetchPreferences() =
        mapCellsPreferences(dataStore.data.first().toPreferences())

    suspend fun getInstalledVersion(): Int {
        return mapCellsPreferences(dataStore.data.first().toPreferences()).versionCode
    }

    suspend fun setInstalledVersion(newVersion: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.INSTALLED_VERSION_CODE] = newVersion
        }
    }

    suspend fun setShowDebugToolsFlag(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DEBUG_TOOLS] = show
        }
    }

    suspend fun setDisablePollFlag(disablePoll: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLE_POLL] = disablePoll
        }
    }

//    /**
//     * Returns Pair<ORDER_BY, DIRECTION> e.g Pair("sort_name", "DESC")
//     */
//    suspend fun getOrderByPair(type: ListType): Pair<String, String> {
//        val currentKey = when (type) {
//            ListType.JOB -> PreferencesKeys.JOB_SORT_BY
//            ListType.TRANSFER -> PreferencesKeys.TRANSFER_SORT_BY
//            ListType.DEFAULT -> PreferencesKeys.DEFAULT_LIST_ORDER
//        }
//        return parseOrder(dataStore.data.first().toPreferences()[currentKey], type)
//    }

    /**
     * Returns Pair<ORDER_BY, DIRECTION> e.g Pair("sort_name", "DESC")
     */
    fun getOrderByPair(currPreferences: CellsPreferences, type: ListType): Pair<String, String> {
        val encoded = when (type) {
            ListType.JOB -> currPreferences.list.jobOrder
            ListType.TRANSFER -> currPreferences.list.transferOrder
            ListType.DEFAULT -> currPreferences.list.order
        }
        return parseOrder(encoded, type)
    }

    suspend fun setOrder(type: ListType, order: String) {
        // edit handles data transactionally, ensuring that if the sort is updated at the same  time from another thread, we won't have conflicts
        dataStore.edit { preferences ->
            val currentKey = when (type) {
                ListType.JOB -> PreferencesKeys.JOB_SORT_BY
                ListType.TRANSFER -> PreferencesKeys.TRANSFER_SORT_BY
                ListType.DEFAULT -> PreferencesKeys.DEFAULT_LIST_ORDER
            }
            preferences[currentKey] = order
        }
    }

    suspend fun setBoolean(key: Preferences.Key<Boolean>, flag: Boolean) {
        dataStore.edit { preferences ->
            preferences[key] = flag
        }
    }

    suspend fun setString(key: Preferences.Key<String>, strValue: String) {
        dataStore.edit { preferences ->
            preferences[key] = strValue
        }
    }

    suspend fun setFilter(type: ListType, filter: String) {
        dataStore.edit { preferences ->
            val currentKey = when (type) {
                ListType.JOB -> PreferencesKeys.JOB_FILTER_BY_STATUS
                ListType.TRANSFER -> PreferencesKeys.TRANSFER_FILTER_BY_STATUS
                else -> throw RuntimeException("Unsupported filter type: ${type.name}")
            }
            preferences[currentKey] = filter
        }
    }

    suspend fun setListLayout(layout: ListLayout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_LIST_LAYOUT] = layout.name
        }
        Log.e(logTag, "New layout : $layout")
    }

    private fun mapCellsPreferences(toPreferences: Preferences): CellsPreferences {
        val currVersion = toPreferences[PreferencesKeys.INSTALLED_VERSION_CODE] ?: -1
        // Show technical pages
        val showDebug = toPreferences[PreferencesKeys.SHOW_DEBUG_TOOLS] ?: noPref.showDebugTools
        val disablePoll = toPreferences[PreferencesKeys.DISABLE_POLL] ?: noPref.disablePoll
        val layout =
            if (ListLayout.GRID.name == toPreferences[PreferencesKeys.DEFAULT_LIST_LAYOUT])
                ListLayout.GRID
            else
                noPref.list.layout
        // List order, layout and filters
        val listPref = ListPreferences(
            order = toPreferences[PreferencesKeys.DEFAULT_LIST_ORDER]
                ?: noPref.list.order,
            layout = layout,
            transferOrder =
            toPreferences[PreferencesKeys.TRANSFER_SORT_BY] ?: noPref.list.transferOrder,
            transferFilter = toPreferences[PreferencesKeys.TRANSFER_FILTER_BY_STATUS]
                ?: noPref.list.transferFilter,
            jobOrder = toPreferences[PreferencesKeys.JOB_SORT_BY] ?: noPref.list.jobOrder,
            jobFilter =
            toPreferences[PreferencesKeys.JOB_FILTER_BY_STATUS] ?: noPref.list.jobFilter,
        )
        // Metered network limitations
        val meteredPref = MeteredNetworkPreferences(
            applyLimits = toPreferences[PreferencesKeys.APPLY_METERED_LIMITATION]
                ?: noPref.meteredNetwork.applyLimits,
            dlThumbs = toPreferences[PreferencesKeys.METERED_DL_THUMBS]
                ?: noPref.meteredNetwork.dlThumbs,
            askBeforeDL = toPreferences[PreferencesKeys.METERED_ASK_B4_DL_FILES]
                ?: noPref.meteredNetwork.askBeforeDL,
            sizeThreshold = toPreferences[PreferencesKeys.METERED_ASK_B4_DL_FILES_SIZE]
                ?: noPref.meteredNetwork.sizeThreshold,
        )
        // Offline settings
        val syncPref = SyncPreferences(
            frequency = toPreferences[PreferencesKeys.SYNC_FREQ] ?: noPref.sync.frequency,
            onNetworkType = toPreferences[PreferencesKeys.SYNC_CONST_NETWORK_TYPE]
                ?: noPref.sync.onNetworkType,
            onCharging = toPreferences[PreferencesKeys.SYNC_CONST_ON_CHARGING]
                ?: noPref.sync.onCharging,
            onBatteryNotLow = toPreferences[PreferencesKeys.SYNC_CONST_ON_BATT_NOT_LOW]
                ?: noPref.sync.onBatteryNotLow,
            onIdle = toPreferences[PreferencesKeys.SYNC_CONST_ON_IDLE] ?: noPref.sync.onIdle
        )
        return CellsPreferences(
            currVersion,
            showDebug,
            disablePoll,
            listPref,
            meteredPref,
            syncPref
        )
    }
}
