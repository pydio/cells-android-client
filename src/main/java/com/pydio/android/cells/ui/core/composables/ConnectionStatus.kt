package com.pydio.android.cells.ui.core.composables

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pydio.android.cells.NetworkStatus
import com.pydio.android.cells.R
import com.pydio.android.cells.Status
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.services.models.isOK
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import org.koin.compose.koinInject

private const val LOG_TAG = "ConnectionStatus.kt"

@Composable
fun ConnectionStatus(
    accountService: AccountService = koinInject(),
    connectionService: ConnectionService = koinInject(),
) {
    val currSession = connectionService.sessionView.collectAsState(initial = null)
    val sessionStatus = connectionService.sessionStateFlow.collectAsState()
//        SessionState(NetworkStatus.OK, true, LoginStatus.Connected)
//    )
    val knownSessions = accountService.getLiveSessions().collectAsState(listOf())

    currSession.value?.let {
        val currState = sessionStatus.value

        when {
            currState.isOK() -> {
                // No header
            }

            currState.networkStatus == NetworkStatus.UNAVAILABLE
            -> ConnectionStatus(
                icon = CellsIcons.NoInternet,
                desc = stringResource(R.string.no_internet),
                type = Status.WARNING
            )

            currState.networkStatus == NetworkStatus.CAPTIVE
            -> ConnectionStatus(
                icon = CellsIcons.CaptivePortal,
                desc = stringResource(R.string.captive_portal),
                type = Status.DANGER
            )

            !currState.isServerReachable -> {
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

            currState.isServerReachable && !currState.loginStatus.isConnected()
            -> {
                Log.e(LOG_TAG, "Credentials Expired ")
                ConnectionStatus(
                    icon = CellsIcons.NoValidCredentials,
                    desc = stringResource(R.string.auth_err_expired),
                    type = Status.DANGER

                )
            }

            // TODO also handle preferences on limited networks
            currState.networkStatus == NetworkStatus.METERED
                    || currState.networkStatus == NetworkStatus.ROAMING
            -> ConnectionStatus(
                icon = CellsIcons.Metered,
                desc = stringResource(R.string.metered_connection),
                type = Status.WARNING
            )

            else -> {
                Log.e(LOG_TAG, "Unexpected status: $currState")

                ConnectionStatus(
                    icon = CellsIcons.Unknown,
                    desc = stringResource(R.string.no_connection_title),
                    type = Status.WARNING
                )
            }
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun ConnectionStatus(
    icon: ImageVector,
    desc: String,
    type: Status = Status.OK
) {

    val tint = when (type) {
        Status.WARNING -> CellsColor.warning
        Status.DANGER -> CellsColor.danger
        Status.OK -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        Status.OK -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(
                horizontal = dimensionResource(R.dimen.margin),
                vertical = dimensionResource(R.dimen.margin_xsmall)
            )
    ) {
        Icon(
            tint = tint,
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(dimensionResource(R.dimen.default_button_inner_size))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.list_item_inner_h_padding)))
        Text(
            text = desc,
            color = tint,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
private fun ConnectionStatusPreview() {
    UseCellsTheme {
        ConnectionStatus(
            icon = CellsIcons.Metered,
            desc = stringResource(id = R.string.metered_connection),
            type = Status.WARNING
        )
    }
}
