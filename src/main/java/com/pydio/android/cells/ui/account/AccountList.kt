package com.pydio.android.cells.ui.account

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.ListContext
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.core.composables.Decorated
import com.pydio.android.cells.ui.core.composables.Type
import com.pydio.android.cells.ui.core.composables.lists.EmptyList
import com.pydio.android.cells.ui.core.composables.lists.WithListTheme
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun AccountList(
    accounts: List<RSessionView>,
    openAccount: (stateID: StateID) -> Unit,
    login: (stateID: StateID, skipVerify: Boolean, isLegacy: Boolean) -> Unit,
    logout: (stateID: StateID) -> Unit,
    forget: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Box(modifier = Modifier.padding(contentPadding)) {
        if (accounts.isEmpty()) {
            EmptyList(
                listContext = ListContext.ACCOUNTS,
                desc = stringResource(R.string.account_list_none),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(.5f)
                    .wrapContentSize(Alignment.Center)
            )
        }
        WithListTheme {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(accounts) { account ->

                    AccountListItem(
                        title = "${account.serverLabel()}",
                        username = account.username,
                        url = account.url,
                        authStatus = account.authStatus,
                        isForeground = account.lifecycleState == AppNames.LIFECYCLE_STATE_FOREGROUND,
                        login = {
                            login(
                                account.getStateID(),
                                account.skipVerify(),
                                account.isLegacy
                            )
                        },
                        logout = { logout(account.getStateID()) },
                        forget = forget,
                        modifier = modifier.clickable {
                            openAccount(StateID(account.username, account.url))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountListItem(
    title: String,
    username: String,
    url: String,
    authStatus: String,
    isForeground: Boolean,
    login: () -> Unit,
    logout: () -> Unit,
    forget: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier
) {

    val buttonAlpha = getFloatResource(LocalContext.current, R.dimen.list_button_alpha)
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
                        .wrapContentSize(Alignment.Center)
                        .size(dimensionResource(R.dimen.list_thumb_icon_size))
                        .alpha(buttonAlpha)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${username}@${url}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val connected = authStatus == AppNames.AUTH_STATUS_CONNECTED
            IconButton(onClick = if (connected) logout else login) {
                Icon(
                    imageVector = if (connected) CellsIcons.Logout else CellsIcons.Login,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }

            IconButton(onClick = { forget(StateID(username, url)) }) {
                Icon(
                    imageVector = CellsIcons.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }

        }
    }
}

@Preview(name = "Light Mode")
@Composable
private fun ForegroundAccountListItemPreview() {
    UseCellsTheme {
        WithListTheme {
            AccountListItem(
                "Cells test server",
                "lea",
                "https://example.com",
                authStatus = AppNames.AUTH_STATUS_CONNECTED,
                isForeground = true,
                {}, {}, {}, Modifier
            )
        }
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
        WithListTheme {
            AccountListItem(
                "Cells test server",
                "lea",
                "https://example.com",
//            authStatus = AppNames.AUTH_STATUS_NO_CREDS,
//            authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
                authStatus = AppNames.AUTH_STATUS_EXPIRED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
                isForeground = false,
                {},
                {},
                {},
                Modifier
            )
        }
    }
}
