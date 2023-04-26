package com.pydio.android.cells.ui.system.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.pydio.android.cells.R
import com.pydio.android.cells.db.preferences.ListPreferences
import com.pydio.android.cells.db.preferences.MeteredNetworkPreferences
import com.pydio.android.cells.db.preferences.SyncPreferences
import com.pydio.android.cells.db.preferences.defaultCellsPreferences
import com.pydio.android.cells.services.PreferencesKeys
import com.pydio.android.cells.ui.core.composables.ListSetting
import com.pydio.android.cells.ui.core.composables.PreferenceDivider
import com.pydio.android.cells.ui.core.composables.PreferenceSectionTitle
import com.pydio.android.cells.ui.core.composables.SwitchSetting
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.system.models.SettingsVM

private const val logTag = "Settings"

@Composable
fun SettingsScreen(
    openDrawer: () -> Unit,
    settingsVM: SettingsVM,
) {
//    val topAppBarState = rememberTopAppBarState()
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = dimensionResource(id = R.dimen.margin_small))

    val cellsPreferences =
        settingsVM.cellsPreferences.collectAsState(initial = defaultCellsPreferences())

    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.action_settings),
                openDrawer = openDrawer,
//                topAppBarState = topAppBarState
            )
        },
    ) { innerPadding ->
        val scrollingState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = dimensionResource(R.dimen.margin_medium))
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start)
                .verticalScroll(scrollingState)
        ) {
            ListSection(settingsVM, cellsPreferences.value.list, modifier)
            PreferenceDivider(modifier)
            MeteredSection(settingsVM, cellsPreferences.value.meteredNetwork, modifier)
            PreferenceDivider(modifier)
            OfflineSection(settingsVM, cellsPreferences.value.sync, modifier)
            PreferenceDivider(modifier)
            TroubleshootingSection(
                settingsVM,
                cellsPreferences.value.showDebugTools,
                cellsPreferences.value.disablePoll,
                modifier
            )
        }
    }
}

@Composable
fun ListSection(settingsVM: SettingsVM, listPreferences: ListPreferences, modifier: Modifier) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_recycler),
        modifier,
    )
    ListSetting(
        stringResource(R.string.order_by_title),
        listPreferences.order,
        keys = stringArrayResource(R.array.order_by_values),
        labels = stringArrayResource(R.array.order_by_labels),
        { settingsVM.setDefaultOrder(it) },
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_choose_list_layout),
        listPreferences.layout.name,
        keys = stringArrayResource(R.array.list_layout_values),
        labels = stringArrayResource(R.array.list_layout_labels),
        { settingsVM.setListLayout(it) },
        modifier,
    )
}

@Composable
fun MeteredSection(
    settingsVM: SettingsVM,
    netPref: MeteredNetworkPreferences,
    modifier: Modifier
) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_metered),
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_apply_metered_limits_title),
        stringResource(R.string.pref_apply_metered_limits_desc),
        netPref.applyLimits,
        { settingsVM.setBooleanFlag(PreferencesKeys.APPLY_METERED_LIMITATION, it) },
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_metered_dl_thumbs_title),
        stringResource(R.string.pref_metered_dl_thumbs_desc),
        netPref.dlThumbs,
        { settingsVM.setBooleanFlag(PreferencesKeys.METERED_DL_THUMBS, it) },
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_on_metered_ask_before_dl_files_title),
        null,
        netPref.askBeforeDL,
        { settingsVM.setBooleanFlag(PreferencesKeys.METERED_ASK_B4_DL_FILES, it) },
        modifier,
    )
    //  Re-Enable this when the settings is really used in the app
//    TextSetting(
//        stringResource(R.string.on_metered_ask_before_dl_files_greater_than_title),
//        " ${netPref.sizeThreshold}",
//        modifier,
//    )
}

@Composable
fun OfflineSection(
    settingsVM: SettingsVM,
    syncPref: SyncPreferences,
    modifier: Modifier
) {

    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_offline),
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_choose_list_offline_frequency_title),
        syncPref.frequency, // frequencyLabel(LocalContext.current.resources),
        keys = stringArrayResource(R.array.offline_frequency_values),
        labels = stringArrayResource(R.array.offline_frequency_labels),
        { settingsVM.setStringPref(PreferencesKeys.SYNC_FREQ, it) },
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_offline_constraint_wifi_title),
        syncPref.onNetworkType,
        keys = stringArrayResource(R.array.network_type_values),
        labels = stringArrayResource(R.array.network_type_labels),
        { settingsVM.setStringPref(PreferencesKeys.SYNC_CONST_NETWORK_TYPE, it) },
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_charging_title),
        stringResource(R.string.pref_offline_constraint_charging_desc),
        syncPref.onCharging,
        { settingsVM.setBooleanFlag(PreferencesKeys.SYNC_CONST_ON_CHARGING, it) },
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_batt_not_low_title),
        stringResource(R.string.pref_offline_constraint_batt_not_low_desc),
        syncPref.onBatteryNotLow,
        { settingsVM.setBooleanFlag(PreferencesKeys.SYNC_CONST_ON_BATT_NOT_LOW, it) },
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_idle_title),
        stringResource(R.string.pref_offline_constraint_idle_desc),
        syncPref.onIdle,
        { settingsVM.setBooleanFlag(PreferencesKeys.SYNC_CONST_ON_IDLE, it) },
        modifier,
    )
}

@Composable
fun TroubleshootingSection(
    settingsVM: SettingsVM,
    showRuntimeTools: Boolean,
    disablePoll: Boolean,
    modifier: Modifier
) {
    TroubleshootingSection(
        showRuntimeTools,
        { show -> settingsVM.setShowRuntimeToolsFlag(show) },
        disablePoll,
        { disable -> settingsVM.setDisblePollFlag(disable) },
        modifier
    )
}

@Composable
fun TroubleshootingSection(
    showRuntimeTools: Boolean,
    onShowRuntimeToolsClick: (Boolean) -> Unit,
    disablePoll: Boolean,
    onDisablePollClick: (Boolean) -> Unit,
    modifier: Modifier
) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_troubleshooting),
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_troubleshooting_show_list_title),
        stringResource(R.string.pref_troubleshooting_show_list_desc),
        showRuntimeTools,
        { onShowRuntimeToolsClick(!showRuntimeTools) },
        modifier,
    )
    // TODO re-enable this when ready
//    if (showRuntimeTools) {
//        SwitchSetting(
//            stringResource(R.string.pref_troubleshooting_disable_poll_title),
//            stringResource(R.string.pref_troubleshooting_disable_poll_desc),
//            disablePoll,
//            { onDisablePollClick(!disablePoll) },
//            modifier,
//        )
//    }
}
