package com.pydio.android.cells.ui.box.account

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
import com.pydio.android.cells.ui.theme.CellsTheme
import com.pydio.android.cells.ui.theme.CellsVectorIcons
import com.pydio.cells.transport.StateID

@Composable
fun TargetAccountList(
    accounts: List<RSessionView>?,
    openAccount: (stateID: StateID) -> Unit,
    doLogin: (stateID: StateID) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
) {

    val alpha = getFloatResource(LocalContext.current, R.dimen.disabled_list_item_alpha)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement

    ) {
        items(accounts ?: listOf()) { account ->
            var currModifier = if (account.authStatus == AppNames.AUTH_STATUS_CONNECTED) {
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
                doLogin = doLogin,
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
    doLogin: (stateID: StateID) -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        tonalElevation = if (isForeground) dimensionResource(R.dimen.list_item_selected_elevation) else 0.dp,
        modifier = modifier
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // The icon
            Decorated(Type.AUTH, authStatus) {
                Icon(
                    imageVector = CellsVectorIcons.Person,
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
                    text = "${login}@${url}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // TODO this does not work yet when remote is Cells (OAuth flow)
//            if (authStatus != AppNames.AUTH_STATUS_CONNECTED) {
//
//                Surface(
//                    modifier = Modifier
//                        .size(40.dp)
//                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
//                        .clickable(onClick = {
//                            doLogin(StateID(login, url))
//                        })
//                        .background(MaterialTheme.colorScheme.error)
//                ) {
//                    Image(
//                        painter = painterResource(R.drawable.ic_baseline_login_24),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .size(48.dp)
//                            .size(dimensionResource(R.dimen.list_thumb_size))
//                            //.clip(CircleShape)
//                            .wrapContentSize(Alignment.Center)
//                    )
//                }
//            }

        }
    }
}


@Preview(name = "Light Mode")
@Composable
private fun ForegroundAccountListItemPreview() {
    CellsTheme {
        TargetAccountListItem(
            "Cells test server",
            "lea",
            "https://example.com",
//            authStatus = AppNames.AUTH_STATUS_NO_CREDS,
//            authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED,
            authStatus = AppNames.AUTH_STATUS_CONNECTED,
//            authStatus = AppNames.AUTH_STATUS_EXPIRED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
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
    CellsTheme {
        TargetAccountListItem(
            "Cells test server",
            "lea",
            "https://example.com",
            authStatus = AppNames.AUTH_STATUS_NO_CREDS,
//            authStatus = AppNames.AUTH_STATUS_UNAUTHORIZED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
//            authStatus = AppNames.AUTH_STATUS_EXPIRED,
//            authStatus = AppNames.AUTH_STATUS_CONNECTED,
            isForeground = false,
            {},
            Modifier
        )
    }
}
