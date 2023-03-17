package com.pydio.android.cells.ui.system.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.preferences.*
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.system.models.SettingsVM
import com.pydio.android.cells.ui.theme.CellsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    openDrawer: () -> Unit,
    settingsVM: SettingsVM,
) {
    val topAppBarState = rememberTopAppBarState()
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
                topAppBarState = topAppBarState
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
            ListSection(cellsPreferences.value.list, modifier)
            PreferenceDivider(modifier)
            MeteredSection(cellsPreferences.value.meteredNetwork, modifier)
            PreferenceDivider(modifier)
            OfflineSection(cellsPreferences.value.sync, modifier)
            PreferenceDivider(modifier)
            TroubleshootingSection(settingsVM, cellsPreferences.value.showDebugTools, modifier)
        }
    }
}

@Composable
fun ListSection(listPreferences: ListPreferences, modifier: Modifier) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_recycler),
        modifier,
    )
    ListSetting(
        stringResource(R.string.order_by_title),
        listPreferences.orderLabel(LocalContext.current.resources),
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_choose_list_layout),
        listPreferences.layout.name,
        modifier,
    )
}

@Composable
fun MeteredSection(netPref: MeteredNetworkPreferences, modifier: Modifier) {

    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_metered),
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_apply_metered_limits_title),
        stringResource(R.string.pref_apply_metered_limits_desc),
        netPref.applyLimits,
        {}, // TODO
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_metered_dl_thumbs_title),
        stringResource(R.string.pref_metered_dl_thumbs_desc),
        netPref.dlThumbs,
        {}, // TODO
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_on_metered_ask_before_dl_files_title),
        null,
        netPref.askBeforeDL,
        {}, // TODO
        modifier,
    )
    TextSetting(
        stringResource(R.string.on_metered_ask_before_dl_files_greater_than_title),
        " ${netPref.sizeThreshold}",
        modifier,
    )
}

@Composable
fun OfflineSection(syncPref: SyncPreferences, modifier: Modifier) {

    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_offline),
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_choose_list_offline_frequency_title),
        syncPref.frequencyLabel(LocalContext.current.resources),
        modifier,
    )
    ListSetting(
        stringResource(R.string.pref_offline_constraint_wifi_title),
        syncPref.onNetworkTypeLabel(LocalContext.current.resources),
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_charging_title),
        stringResource(R.string.pref_offline_constraint_charging_desc),
        syncPref.onCharging,
        {}, // TODO
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_batt_not_low_title),
        stringResource(R.string.pref_offline_constraint_batt_not_low_desc),
        syncPref.onBatteryNotLow,
        {}, // TODO
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_offline_constraint_idle_title),
        stringResource(R.string.pref_offline_constraint_idle_desc),
        syncPref.onIdle,
        {}, // TODO
        modifier,
    )
}

@Composable
fun TroubleshootingSection(settingsVM: SettingsVM, showSystemPages: Boolean, modifier: Modifier) {
    TroubleshootingSection(
        showSystemPages,
        { show -> settingsVM.setShowRuntimeToolsFlag(show) },
        modifier
    )
}

@Composable
fun TroubleshootingSection(
    showSystemPages: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier
) {
    PreferenceSectionTitle(
        stringResource(R.string.pref_category_title_troubleshooting),
        modifier,
    )
    SwitchSetting(
        stringResource(R.string.pref_troubleshooting_show_list_title),
        stringResource(R.string.pref_troubleshooting_show_list_desc),
        showSystemPages,
        { onClick(!showSystemPages) },
        modifier,
    )
}

@Composable
fun PreferenceSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ListSetting(label: String, currValue: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = currValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SwitchSetting(
    label: String,
    description: String?,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = label },
            checked = isSelected,
            onCheckedChange = { onItemClick() }
        )
    }

}

@Composable
fun TextSetting(label: String, currValue: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface

        )
        Text(
            text = currValue,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PreferenceDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        color = color.copy(alpha = .6f),
        thickness = 1.dp,
    )
}

@Preview(name = "Light Mode Item")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode Item"
)
@Composable
private fun PreferenceItemPreview() {

    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = dimensionResource(id = R.dimen.margin_small))
    CellsTheme {
        Column(
            horizontalAlignment = Alignment.Start,
        ) {

            PreferenceSectionTitle("Customise List", modifier)
            ListSetting("Sort Order", "Modified (newest first)", modifier)
            SwitchSetting(
                "Download thumbs",
                "Also retrieve thumbnails when on a metered network",
                true,
                {},
                modifier,
            )
            TextSetting(label = "When file is greater than (in MB)", currValue = "2")
        }
    }
}

@Preview(name = "Light Mode List")
@Composable
private fun PreferenceListPreview() {
    CellsTheme {
//         PreferenceList() {}
    }
}
