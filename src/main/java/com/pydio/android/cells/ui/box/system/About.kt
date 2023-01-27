package com.pydio.android.cells.ui.box.system

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.BuildConfig
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.box.common.DefaultTitleText
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.getTimestampAsString
import com.pydio.cells.transport.ClientData

@Composable
fun AboutScreen(
    onUriClick: () -> Unit,
    onEmailClick: () -> Unit,
) {
    val data = ClientData.getInstance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText(stringResource(R.string.about_version_title))
        VersionCard(
            version = data.version,
            code = BuildConfig.VERSION_CODE.toString(),
            lastUpdateTime = getTimestampAsString(data.lastUpdateTime),
            onUriClick = onUriClick,
        )

        DefaultTitleText(stringResource(R.string.about_help_title))
        MigrateDBCard(
            stringResource(R.string.about_page_get_help),
            stringResource(R.string.help_button_support),
            onEmailClick = onEmailClick,
        )
    }
}

@Composable
private fun VersionCard(
    version: String,
    code: String,
    lastUpdateTime: String,
    onUriClick: () -> Unit,
) {
    // TODO this had rounded corners
    Surface(
        tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.text_padding_medium),
                vertical = dimensionResource(R.dimen.text_padding_small)
            )
            .wrapContentWidth(Alignment.CenterHorizontally)
//            // must be last
//            .shadow(elevation = dimensionResource(R.dimen.grid_ws_card_elevation))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_small),
                    vertical = dimensionResource(R.dimen.card_padding),
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = stringResource(R.string.version_name_display, version),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.text_padding_small))
            )
            Text(
                text = stringResource(R.string.version_code_display, code),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.last_update_time_display, lastUpdateTime),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_page_copyright),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.copyright_string),
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                text = stringResource(R.string.open_website_button),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onUriClick() }
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.card_padding))
                    .padding(
                        top = dimensionResource(R.dimen.text_padding_medium),
                        bottom = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun MigrateDBCard(
    message: String,
    emailLabel: String,
    onEmailClick: () -> Unit,
) {

    Surface(
        tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.text_padding_medium),
                vertical = dimensionResource(R.dimen.text_padding_small)
            )
            .wrapContentWidth(Alignment.CenterHorizontally)
//            .shadow(elevation = dimensionResource(R.dimen.grid_ws_card_elevation))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_small),
                    vertical = dimensionResource(R.dimen.card_padding),
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.text_padding_small))
            )
            Text(
                text = emailLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable { onEmailClick() }
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.card_padding))
                    .padding(
                        top = dimensionResource(R.dimen.text_padding_medium),
                        bottom = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )
        }
    }
}


//@Composable
//private fun TitleText(text: String) {
//    Text(
//        text = text.uppercase(),
//        color = MaterialTheme.colorScheme.primary,
//        style = MaterialTheme.typography.titleSmall,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = dimensionResource(R.dimen.card_padding))
//            .padding(top = dimensionResource(R.dimen.margin_medium))
//            .wrapContentWidth(Alignment.Start)
//            .alpha(.8f)
//    )
//}


@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun AboutScreenPreview() {
    CellsTheme {
        AboutScreen({}, {})
    }
}

@Preview
@Composable
private fun VersionCardPreview() {
    CellsTheme {
        VersionCard(
            "3.0.4", "131", "11 Jan 2023", {}
        )
    }
}

@Preview
@Composable
private fun TroubleShootingCardPreview() {
    CellsTheme {
        MigrateDBCard(
            "If you cannot get this application to work correctly....",
            "Contact Pydio Support",
            {}
        )
    }
}
