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
import com.pydio.android.cells.services.ConnectionService
import com.pydio.android.cells.ui.login.LoginDestinations
import com.pydio.android.cells.ui.theme.CellsColor
import com.pydio.android.cells.ui.theme.CellsIcons
import com.pydio.android.cells.ui.theme.UseCellsTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private enum class Status {
    OK, WARNING, DANGER
}

private const val logTag = "InternetBanner"

@Composable
fun WithInternetBanner(
    contentPadding: PaddingValues,
    connectionService: ConnectionService,
    navigateTo: (String) -> Unit,
    content: @Composable () -> Unit
) {
    // TODO add bottom sheet
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        InternetBanner(connectionService, navigateTo)
        content()
    }
}

@Composable
private fun InternetBanner(
    connectionService: ConnectionService,
    navigateTo: (String) -> Unit,
) {
    val sessionStatus = connectionService.sessionStatusFlow
        .collectAsState(initial = SessionStatus.OK)

    val scope = rememberCoroutineScope()

    if (SessionStatus.OK != sessionStatus.value) {
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
            -> ConnectionStatus(
                icon = CellsIcons.ServerUnreachable,
                desc = stringResource(R.string.server_unreachable)
            )

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
                type = Status.WARNING,
                onClick = {
                    scope.launch {
                        Log.e(logTag, "Launching relog")
                        connectionService.sessionView.collectLatest { sv ->
                            sv?.let {
                                val route = if (it.isLegacy) {
                                    LoginDestinations.P8Credentials.createRoute(
                                        it.getStateID(),
                                        it.skipVerify()
                                    )
                                } else {
                                    Log.e(logTag, "Creating route for ${it.accountID}")
                                    LoginDestinations.LaunchAuthProcessing.createRoute(
                                        it.getStateID(),
                                        it.skipVerify()
                                    )
                                }
                                navigateTo(route)
                            } ?: run {
                                Log.e(logTag, "... Cannot launch, empty session view")
                            }
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
        else -> MaterialTheme.colorScheme.onSurface
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        else -> MaterialTheme.colorScheme.surface
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
    type: Status = Status.WARNING,
    onClick: () -> Unit
) {

    val tint = when (type) {
        Status.WARNING -> CellsColor.warning
        Status.DANGER -> CellsColor.danger
        else -> MaterialTheme.colorScheme.onSurface
    }

    val bg = when (type) {
        Status.WARNING -> CellsColor.warning.copy(alpha = .1f)
        Status.DANGER -> CellsColor.danger.copy(alpha = .1f)
        else -> MaterialTheme.colorScheme.surface
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
