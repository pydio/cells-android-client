package com.pydio.android.cells.ui.share.composables

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.core.composables.Decorated
import com.pydio.android.cells.ui.core.composables.Type
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun TargetAccountList(
    accounts: List<RSessionView>,
    openAccount: (StateID) -> Unit,
    doLogin: (StateID, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {

    val alpha = getFloatResource(LocalContext.current, R.dimen.disabled_list_item_alpha)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.list_vertical_padding)),
    ) {
        items(accounts) { account ->
            val currModifier = if (account.isLoggedIn()) {
                modifier.clickable {
                    openAccount(StateID(account.username, account.url))
                }
            } else {
                modifier.alpha(alpha)
            }

            TargetAccountListItem(
                title = "${account.serverLabel()}",
                login = account.username,
                url = account.url,
                authStatus = account.authStatus,
                isForeground = account.lifecycleState == AppNames.LIFECYCLE_STATE_FOREGROUND,
                doLogin = { doLogin(account.getStateID(), account.skipVerify(), account.isLegacy) },
                modifier = currModifier
            )
        }
    }
}

@Composable
private fun TargetAccountListItem(
    title: String,
    login: String,
    url: String,
    authStatus: String,
    isForeground: Boolean,
    doLogin: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        tonalElevation = if (isForeground) dimensionResource(R.dimen.list_item_selected_elevation) else 0.dp,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Decorated(Type.AUTH, authStatus) {
                Icon(
                    imageVector = CellsIcons.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.list_thumb_size))
                        .alpha(.8f)
                )
            }

            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.list_thumb_margin)))

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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${login}@${url}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (authStatus != LoginStatus.Connected.id) {
                IconButton(onClick = doLogin) {
                    Icon(
                        imageVector = CellsIcons.Login,
                        contentDescription = stringResource(R.string.action_re_log),
                        modifier = Modifier.size(dimensionResource(R.dimen.list_trailing_icon_size))
                    )
                }
            }
        }
    }
}


@Preview(name = "Light Mode")
@Composable
private fun ForegroundAccountListItemPreview() {
    UseCellsTheme {
        TargetAccountListItem(
            "Cells test server",
            "lea",
            "https://example.com",
            authStatus = LoginStatus.Connected.id,
            isForeground = true,
            {},
            Modifier
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun AccountListItemPreview() {
    UseCellsTheme {
        TargetAccountListItem(
            "Cells test server",
            "lea",
            "https://example.com",
            authStatus = LoginStatus.NoCreds.id,
            isForeground = false,
            {},
            Modifier
        )
    }
}
