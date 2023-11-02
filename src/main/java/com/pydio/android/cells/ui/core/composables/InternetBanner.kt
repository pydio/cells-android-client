package com.pydio.android.cells.ui.core.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.R
import com.pydio.android.cells.SessionStatus
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.ErrorService
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class Status {
    OK, WARNING, DANGER
}

private const val LOG_TAG = "InternetBanner.kt"

@Composable
fun WithInternetBanner(
    contentPadding: PaddingValues,
    connectionService: ConnectionService,
    navigateTo: (String) -> Unit,
    accountService: AccountService = koinInject(),
    errorService: ErrorService = koinInject(),
    content: @Composable () -> Unit
) {

    val localNavigateTo: (String) -> Unit =
        {    // clean error stack before launching the relog process
            errorService.clearStack()
            navigateTo(it)
        }

    // TODO add bottom sheet
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        InternetBanner(accountService, connectionService, localNavigateTo)
        content()
    }
}

@Composable
private fun InternetBanner(
    accountService: AccountService,
    connectionService: ConnectionService,
    navigateTo: (String) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val currSession = connectionService.sessionView.collectAsState(initial = null)
    val sessionStatus = connectionService.sessionStatusFlow
        .collectAsState(initial = SessionStatus.OK)
    val knownSessions = accountService.getLiveSessions().collectAsState(listOf())

    if (SessionStatus.OK != sessionStatus.value && currSession.value != null) {
        when (sessionStatus.value) {
            SessionStatus.NO_INTERNET
            -> ConnectionStatus(
                icon = CellsIcons.NoInternet,
                desc = stringResource(R.string.no_internet)
            )

            SessionStatus.CAPTIVE
            -> ConnectionStatus(
                icon = CellsIcons.CaptivePortal,
                desc = stringResource(R.string.captive_portal)
            )

            SessionStatus.SERVER_UNREACHABLE
            -> {
                if (knownSessions.value.isEmpty()) {
                    ConnectionStatus(
                        icon = CellsIcons.ServerUnreachable,
                        desc = stringResource(R.string.no_account)
                    )
                } else {
                    ConnectionStatus(
                        icon = CellsIcons.ServerUnreachable,
                        desc = stringResource(R.string.server_unreachable)
                    )
                }
            }

            SessionStatus.METERED,
            SessionStatus.ROAMING
            -> ConnectionStatus(
                icon = CellsIcons.Metered,
                desc = stringResource(R.string.metered_connection),
                type = Status.WARNING
            )

            SessionStatus.CAN_RELOG
            -> CredExpiredStatus(
                icon = CellsIcons.NoValidCredentials,
                desc = stringResource(R.string.auth_err_expired),
                onClick = {
                    scope.launch {
                        currSession.value?.let {
                            val route = if (it.isLegacy) {
                                Log.i(LOG_TAG, "... Launching re-log on P8 for ${it.accountID}")
                                LoginDestinations.P8Credentials.createRoute(
                                    it.getStateID(),
                                    it.skipVerify(),
                                    AuthService.LOGIN_CONTEXT_BROWSE
                                )
                            } else {
                                Log.i(LOG_TAG, "... Launching re-log on Cells for ${it.accountID} from ${it.getStateID()}")
                                LoginDestinations.LaunchAuthProcessing.createRoute(
                                    it.getStateID(),
                                    it.skipVerify(),
                                    AuthService.LOGIN_CONTEXT_BROWSE
                                )
                            }
                            navigateTo(route)
                        } ?: run {
                            Log.e(LOG_TAG, "... Cannot launch, empty session view")
                        }
                    }
                }
            )
            //ConnectionVM.SessionStatus.NOT_LOGGED_IN,
            else -> ConnectionStatus(
                icon = CellsIcons.NoValidCredentials,
                desc = stringResource(id = R.string.auth_err_no_token),
                type = Status.DANGER
            )
        }
    }
}

@Composable
private fun ConnectionStatus(icon: ImageVector, desc: String, type: Status = Status.OK) {

    val tint = when (type) {
        Status.WARNING -> CellsColor.warning
        Status.DANGER -> CellsColor.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_xxsmall)
            )
    ) {
        Icon(
            tint = tint,
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(id = R.dimen.list_thumb_decorator_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(
            text = desc,
            color = tint,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CredExpiredStatus(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit
) {

    val tint = CellsColor.warning
    val bg = CellsColor.warning.copy(alpha = .1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(
                horizontal = dimensionResource(R.dimen.margin_small),
                vertical = dimensionResource(R.dimen.margin_xxsmall)
            )
    ) {
        Icon(
            tint = tint,
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(id = R.dimen.list_trailing_icon_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(
            text = desc,
            color = tint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text(
                text = stringResource(R.string.launch_auth).uppercase(),
                color = tint,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview
@Composable
private fun ConnectionStatusPreview() {
    UseCellsTheme {
        ConnectionStatus(
            icon = CellsIcons.Metered,
            desc = stringResource(id = R.string.metered_connection),
            Status.WARNING
        )
    }
}
