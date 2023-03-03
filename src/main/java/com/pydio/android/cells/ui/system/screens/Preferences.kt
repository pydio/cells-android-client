package com.pydio.android.cells.ui.system.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.DefaultTitleText
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

    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.action_settings),
                openDrawer = openDrawer,
                topAppBarState = topAppBarState
            )
        },
    ) { innerPadding ->

        // See res/xml/preferences.xml for the settings that were present in v3.0

        val resources = LocalContext.current.resources
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start)
        ) {
            item {
                DefaultTitleText(stringResource(R.string.about_version_title))
            }

            item {
                DefaultTitleText(stringResource(R.string.about_help_title))
            }
        }
    }
}

@Preview(name = "Light Mode Item")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode Item"
)
@Composable
private fun PreferenceItemPreview() {
    CellsTheme {
//         PreferenceItem() {}
    }
}

@Preview(name = "Light Mode List")
@Composable
private fun PreferenceListPreview() {
    CellsTheme {
//         PreferenceList() {}
    }
}
