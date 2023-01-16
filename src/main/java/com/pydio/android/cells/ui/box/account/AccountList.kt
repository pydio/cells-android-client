package com.pydio.android.cells.ui.box.account

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun AccountList(
    accounts: List<RSessionView>?,
    openAccount: (stateID: StateID) -> Unit,
    login: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(accounts ?: listOf()) { account ->
            AccountListItem(
                title = "${account.serverLabel()}",
                login = account.username,
                url = account.url,
                modifier = modifier.clickable {
                    openAccount(StateID(account.username, account.url))
                }
            )
        }
    }
}

@Composable
private fun AccountListItem(
    title: String,
    login: String,
    url: String,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = dimensionResource(R.dimen.list_item_elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.card_padding))
            .wrapContentWidth(Alignment.CenterHorizontally)
    ) {

        Row(modifier = Modifier.padding(all = 8.dp)) {

            Surface(
                modifier = Modifier
                    .size(40.dp)
                    // .size(dimensionResource(R.dimen.list_thumb_size))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                    .background(MaterialTheme.colorScheme.error)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_person_24),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .size(48.dp)
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        //.clip(CircleShape)
                        .wrapContentSize(Alignment.Center)
                )
                Image(
                    painter = painterResource(R.drawable.ic_baseline_check_24),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Green),
                    modifier = Modifier // .fillMaxSize()
                        //.size(dimensionResource(R.dimen.list_thumb_decorator_size))
                        .size(12.dp)
                        .wrapContentSize(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.list_thumb_margin)))

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_padding),
                        vertical = dimensionResource(R.dimen.margin_xsmall)
                    )
                    .wrapContentWidth(Alignment.Start)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${login}@${url}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
private fun AccountListItemPreview(
) {
    CellsTheme {
        AccountListItem("Cells test server", "lea", "https://example.com", Modifier)
    }
}

