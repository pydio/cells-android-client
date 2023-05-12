package com.pydio.android.cells.ui.system.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.BuildConfig
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.nav.DefaultTopAppBar
import com.pydio.android.cells.ui.core.nav.openExternalURL
import com.pydio.android.cells.ui.core.nav.sendSupportEmail
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.android.cells.utils.getTimestampAsString
import com.pydio.cells.transport.ClientData

@Composable
fun AboutScreen(
    openDrawer: () -> Unit,
    launchIntent: (Intent, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val data = ClientData.getInstance()

    val pydioSiteUrl = stringResource(R.string.main_website)

    Scaffold(
        topBar = {
            DefaultTopAppBar(
                title = stringResource(R.string.about_version_title),
                openDrawer = openDrawer,
            )
        },
        modifier = modifier
    ) { innerPadding ->

        val resources = LocalContext.current.resources
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(dimensionResource(id = R.dimen.margin_small))
                .verticalScroll(scrollState)
        ) {
            VersionCard(
                version = data.version,
                code = BuildConfig.VERSION_CODE.toString(),
                onUriClick = {
                    val intent = openExternalURL(pydioSiteUrl)
                    launchIntent(intent, false, false)
                },
                lastUpdateTime = getTimestampAsString(data.lastUpdateTime),
            )
            TroubleShootingCard(
                stringResource(R.string.about_page_get_help),
                stringResource(R.string.help_button_support),
                onEmailClick = {
                    val intent = sendSupportEmail(resources)
                    launchIntent(intent, false, false)
                },
            )
        }
    }
}

@Composable
private fun VersionCard(
    version: String,
    code: String,
    onUriClick: () -> Unit,
    lastUpdateTime: String,
) {
    ElevatedCard(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.margin))
    ) {

        // TODO enhance: this has not the expected behaviour, seems like chaining padding
        //  does not replace the former value but rather adds to it. Check and fix.
        val baseModifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin))
        val lineModifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.margin),
                vertical = dimensionResource(R.dimen.margin_xsmall),
            )
        Text(
            text = stringResource(R.string.about_version_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = baseModifier.padding(bottom = dimensionResource(R.dimen.margin_small))
        )

        val desc = stringResource(
            R.string.version_name_display,
            version
        ) + "\n" + stringResource(
            R.string.version_code_display,
            code
        ) + "\n" + stringResource(
            R.string.last_update_time_display,
            lastUpdateTime
        ) + "\n\n" + stringResource(
            R.string.about_page_copyright
        ) + "\n" + stringResource(R.string.copyright_string)

        VersionLine(desc, lineModifier)

//        VersionLine(stringResource(R.string.version_name_display, version), lineModifier)
//        VersionLine(stringResource(R.string.version_code_display, code), lineModifier)
//        VersionLine(stringResource(R.string.last_update_time_display, lastUpdateTime), lineModifier)
//        VersionLine(stringResource(R.string.about_page_copyright), lineModifier)
//        VersionLine(stringResource(R.string.copyright_string), lineModifier)

        Button(
            onClick = onUriClick,
            modifier = baseModifier
                .padding(top = dimensionResource(R.dimen.margin_small))
                .wrapContentWidth(Alignment.End)
        ) {
            Text(stringResource(R.string.open_website_button))
        }
    }
}

@Composable
fun VersionLine(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier
    )
}

@Composable
private fun TroubleShootingCard(
    message: String,
    emailLabel: String,
    onEmailClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.padding(dimensionResource(id = R.dimen.margin))
    ) {
        val baseModifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin))
        Text(
            text = stringResource(R.string.about_help_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = baseModifier
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = baseModifier.padding(
                top = dimensionResource(R.dimen.margin_xsmall),
                bottom = dimensionResource(id = R.dimen.margin_small),
            )
        )
        Button(
            onClick = onEmailClick,
            modifier = baseModifier.wrapContentWidth(Alignment.End)
        ) {
            Text(text = emailLabel)
        }
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun AboutScreenPreview() {
    UseCellsTheme {
        AboutScreen({}, { _, _, _ -> })
    }
}

@Preview
@Composable
private fun VersionCardPreview() {
    UseCellsTheme {
        VersionCard(
            "3.0.4", "131", {}, "11 Jan 2023"
        )
    }
}

@Preview
@Composable
private fun TroubleShootingCardPreview() {
    UseCellsTheme {
        TroubleShootingCard(
            "If you cannot get this application to work correctly....",
            "Contact Pydio Support"
        ) {}
    }
}
