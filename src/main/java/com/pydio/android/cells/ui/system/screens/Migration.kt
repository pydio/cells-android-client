package com.pydio.android.cells.ui.box.system

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pydio.android.cells.ui.theme.CellsTheme

@Composable
fun PrepareMigration(
    oldCodeVersion: Int,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.Start)
    ) {
        DefaultTitleText("Migrating from v$oldCodeVersion")
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
                    text = "",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.text_padding_small))
                )
                LinearProgressIndicator(
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
        DefaultTitleText("Migrating from v$oldCodeVersion")
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
    oldCodeVersion: Int,
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
        DefaultTitleText("Migrating from v$oldCodeVersion")
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

                    Row() {
                        Text(
                            text = "Skip",
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
                            text = "Launch Sync",
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
                        text = "Start using the app!",
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
//    CellsTheme {
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
    CellsTheme {
        AfterLegacyMigration(43, 8, {}, {})
    }
}

