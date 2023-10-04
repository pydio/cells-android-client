package com.pydio.android.cells.ui.core.nav

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme

@Composable
fun AccountHeader(
    username: String,
    address: String,
    openAccounts: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.clickable { openAccounts() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = dimensionResource(R.dimen.card_padding),
                    vertical = dimensionResource(R.dimen.margin_xsmall)
                )
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        IconButton(
            onClick = { openAccounts() },
            modifier = Modifier
                .size(dimensionResource(R.dimen.default_button_size))
                .clip(CircleShape)
                .background(
                    SolidColor(MaterialTheme.colorScheme.secondaryContainer),
                    CircleShape,
                    0.8f
                )
        ) {
            Icon(
                imageVector = CellsIcons.SwitchAccount,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(all = dimensionResource(R.dimen.margin_xxsmall))
                    .size(dimensionResource(R.dimen.default_button_inner_size))
            )
        }
    }
}

@Composable
fun AccountRailHeader(
    username: String,
    address: String,
    openAccounts: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {

            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { openAccounts() },
                modifier = Modifier
                    .padding(
                        top = dimensionResource(R.dimen.margin_xxsmall),
                        start = dimensionResource(R.dimen.margin_xxsmall),
                        end = dimensionResource(R.dimen.margin_xxsmall),
                        bottom = dimensionResource(R.dimen.margin_medium),
                    )
                    .clickable { openAccounts() }
                    .size(dimensionResource(R.dimen.default_button_size))
                    .clip(CircleShape)
                    .background(
                        SolidColor(MaterialTheme.colorScheme.secondaryContainer),
                        CircleShape,
                        0.8f
                    )
            ) {
                Icon(
                    imageVector = CellsIcons.SwitchAccount,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(dimensionResource(R.dimen.default_button_inner_size))
                )
            }
        }
        Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(name = "Account Header Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Account Header Dark Mode"
)
@Composable
private fun AccountHeaderPreview() {
    UseCellsTheme {
        AccountHeader(
            "alice",
            "https://www.example.com",
            { },
            Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "RailHeader Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "RailHeader Dark Mode"
)
@Composable
private fun RailHeaderPreview() {
    UseCellsTheme {
        AccountRailHeader(
            "alice",
            "https://www.example.com",
            { },
            Modifier
                .width(200.dp)
                .padding(dimensionResource(R.dimen.margin)),
        )
    }
}