package com.pydio.android.cells.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.pydio.android.cells.BuildConfig
import com.pydio.android.cells.R
import com.pydio.android.cells.databinding.FragmentAboutBinding
import com.pydio.android.cells.ui.ActiveSessionViewModel
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.utils.getOSCurrentVersion
import com.pydio.android.cells.utils.getTimestampAsENString
import com.pydio.android.cells.utils.getTimestampAsString
import com.pydio.cells.transport.ClientData
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AboutFragment : Fragment() {

    private val logTag = AboutFragment::class.simpleName

    // We inject this to trigger state saving so that we stay on this page upon screen rotation.
    // This is not used in the code, so we add the suppress annotation above
    private val activeSessionVM by sharedViewModel<ActiveSessionViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        val binding: FragmentAboutBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_about, container, false
        )

        val onUriClick = {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(resources.getString(R.string.main_website))
            startActivity(intent)
        }

        val onEmailClick: () -> Unit = {
            val data = ClientData.getInstance()
            val format = resources.getString(R.string.app_info)
            val appInfo = String.format(
                format,
                data.versionCode,
                data.version,
                getTimestampAsENString(data.buildTimestamp),
                getOSCurrentVersion(),
            )
            val summary = "\n\nPlease describe your problem (in English): \n"

            val intent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
                .putExtra(Intent.EXTRA_TEXT, appInfo + summary)
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(resources.getString(R.string.support_email)))
            if (activity?.packageManager?.resolveActivity(intent, 0) != null) {
                startActivity(intent)
            } else {
                Log.e(logTag, "Could not trigger email for: $appInfo")
            }
        }

        binding.apply {
            composeAboutPage.setContent {
                CellsTheme {
                    AboutPage(onUriClick = onUriClick, onEmailClick = onEmailClick)
                }
            }
        }
        return binding.root
    }
}

@Composable
private fun AboutPage(
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
        TitleText(stringResource(R.string.about_version_title))
        VersionCard(
            version = data.version,
            code = BuildConfig.VERSION_CODE.toString(),
            relDate = getTimestampAsString(data.buildTimestamp),
            onUriClick = onUriClick,
        )

        TitleText(stringResource(R.string.about_help_title))
        TroubleShootingCard(
            stringResource(R.string.about_page_get_help),
            stringResource(R.string.help_button_support),
            onEmailClick = onEmailClick,
        )
    }
}

@Composable
private fun TitleText(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .padding(top = dimensionResource(R.dimen.margin_medium))
            .wrapContentWidth(Alignment.Start)
            .alpha(.8f)
    )
}

@Composable
private fun VersionCard(
    version: String,
    code: String,
    relDate: String,
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
                text = stringResource(R.string.version_date_display, relDate),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.about_page_copyright, relDate),
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
private fun TroubleShootingCard(
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

@Preview
@Composable
private fun AboutPagePreview() {
    CellsTheme {
        AboutPage({}, {})
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
        TroubleShootingCard(
            "If you cannot get this application to work correctly....",
            "Contact Pydio Support",
            {}
        )
    }
}
