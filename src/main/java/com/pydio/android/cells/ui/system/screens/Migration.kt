package com.pydio.android.cells.ui.system.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.core.composables.DefaultTitleText
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.ClientData

@Composable
fun PrepareMigration() {
    // An empty box so that the user thinks he is still on the splash screen
    // until we are really sure we do need a migration (typically, we pass by here on fresh install)
    Box(Modifier.fillMaxSize())
}

@Composable
fun MigrateFromV2(
    oldCodeVersion: Int,
    status: String,
    dr: Float,
    nr: Float,
) {

    val progress = remember(dr, nr) {
        derivedStateOf {
            dr.div(nr)
        }
    }

    val tmpProgress = animateFloatAsState(
        targetValue = progress.value,
        animationSpec = tween(2000)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText(
            text = "Migrating from v$oldCodeVersion",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.card_padding))
                .padding(top = dimensionResource(R.dimen.margin_medium))
        )
        Surface(
            tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_medium),
                    vertical = dimensionResource(R.dimen.text_padding_small)
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.text_padding_small),
                        vertical = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.migration_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )
                LinearProgressIndicator(
                    progress = tmpProgress.value,
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.margin_medium))
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun AfterLegacyMigration(
    offlineRootNb: Int,
    browse: () -> Unit,
    launchSyncAndBrowse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText("Migrated to v${ClientData.getInstance().versionCode}")
        Surface(
            tonalElevation = dimensionResource(R.dimen.grid_ws_card_elevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.text_padding_medium),
                    vertical = dimensionResource(R.dimen.text_padding_small)
                )
                .wrapContentWidth(Alignment.CenterHorizontally)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.text_padding_small),
                        vertical = dimensionResource(R.dimen.text_padding_small),
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = stringResource(R.string.post_migration_title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )

                if (offlineRootNb > 0) {
                    Text(
                        text = stringResource(R.string.post_migration_sync_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                    )

                    Row {
                        Text(
                            text = stringResource(R.string.button_skip),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { browse() }
                                .weight(.5f)
                                .padding(horizontal = dimensionResource(R.dimen.card_padding))
                                .padding(
                                    top = dimensionResource(R.dimen.text_padding_medium),
                                    bottom = dimensionResource(R.dimen.text_padding_small),
                                )
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = stringResource(R.string.button_launch_sync),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { launchSyncAndBrowse() }
                                .weight(.5f)
                                .padding(horizontal = dimensionResource(R.dimen.card_padding))
                                .padding(
                                    top = dimensionResource(R.dimen.text_padding_medium),
                                    bottom = dimensionResource(R.dimen.text_padding_small),
                                )
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.action_open_after_migration),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clickable { browse() }
                            .weight(1f)
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
    }
}

//@Preview(name = "MigrateFromV2 - Light")
//@Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "MigrateFromV2 - Dark"
//)
//@Composable
//private fun MigrateFromV2Preview() {
//   UseCellsTheme {
//        MigrateFromV2(43, "Running... ", 0.7f)
//    }
//}

@Preview(name = "AfterLegacyMigration Light")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "AfterLegacyMigration Dark"
)
@Composable
private fun AfterLegacyMigrationPreview() {
    UseCellsTheme {
        AfterLegacyMigration(8, {}, {})
    }
}
