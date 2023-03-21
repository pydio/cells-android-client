package com.pydio.android.cells.ui.account

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.R
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.ui.core.composables.Decorated
import com.pydio.android.cells.ui.core.composables.Type
import com.pydio.android.cells.ui.core.getFloatResource
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.cells.transport.StateID

@Composable
fun AccountList(
    accounts: List<RSessionView>?,
    openAccount: (stateID: StateID) -> Unit,
    login: (stateID: StateID, skipVerify: Boolean, isLegacy: Boolean) -> Unit,
    logout: (stateID: StateID) -> Unit,
    forget: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical,
) {

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement

    ) {
        items(accounts ?: listOf()) { account ->

            AccountListItem(
                title = "${account.serverLabel()}",
                username = account.username,
                url = account.url,
                authStatus = account.authStatus,
                // isForeground = currAccountID == account.getStateID(),
                isForeground = account.lifecycleState == AppNames.LIFECYCLE_STATE_FOREGROUND,
                login = { login(account.getStateID(), account.skipVerify(), account.isLegacy) },
                logout = { logout(account.getStateID()) },
                forget = forget,
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {

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
                modifier = modifier
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
                    text = "${username}@${url}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val btnVectorImg: ImageVector
            val btnModifier: Modifier
            when (authStatus) {
                AppNames.AUTH_STATUS_CONNECTED -> {
                    btnVectorImg = CellsIcons.Logout
                    btnModifier = Modifier.clickable { logout() }
                }
                else -> {
                    btnVectorImg = CellsIcons.Login
                    btnModifier = Modifier.clickable { login() }
                }
            }

            Surface(
                modifier = btnModifier
                    .padding(horizontal = dimensionResource(id = R.dimen.margin_xsmall))
                    .alpha(buttonAlpha)
            ) {
                Icon(
                    imageVector = btnVectorImg,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.list_trailing_icon_size))
                )
            }

            Surface(
                modifier = Modifier
                    .clickable { forget(StateID(username, url)) }
                    .padding(
                        start = dimensionResource(id = R.dimen.margin_xsmall),
                        end = dimensionResource(id = R.dimen.margin_small)
                    )
                    .alpha(buttonAlpha)
            ) {
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
    CellsTheme {
        AccountListItem(
            "Cells test server",
            "lea",
            "https://example.com",
            authStatus = AppNames.AUTH_STATUS_NO_CREDS,
//            authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
//            authStatus = AppNames.AUTH_STATUS_EXPIRED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
            isForeground = true,
            {},
            {},
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
    CellsTheme {
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
